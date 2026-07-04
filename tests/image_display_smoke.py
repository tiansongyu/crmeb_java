#!/usr/bin/env python3
import argparse
import json
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request


DEFAULT_TIMEOUT = 15
IMAGE_RE = re.compile(r"(https?://[^'\"(),\s]+/)?(?:undefined)?/?crmebimage/[^'\"(),\s]+")
PRIVATE_HOST_RE = re.compile(r"^(localhost|127\.|10\.|192\.168\.|172\.(1[6-9]|2\d|3[0-1])\.)")
PNG_1X1 = (
    b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01"
    b"\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\nIDATx\x9cc\xf8\x0f"
    b"\x00\x01\x01\x01\x00\x18\xdd\x8d\xb0\x00\x00\x00\x00IEND\xaeB`\x82"
)


def request(method, url, body=None, headers=None, timeout=DEFAULT_TIMEOUT):
    req_headers = {"User-Agent": "crmeb-image-smoke/1.0"}
    if headers:
        req_headers.update(headers)
    req = urllib.request.Request(url, data=body, headers=req_headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.headers.get("Content-Type", ""), resp.read()
    except urllib.error.HTTPError as exc:
        return exc.code, exc.headers.get("Content-Type", ""), exc.read()
    except Exception as exc:
        return 0, "", str(exc).encode("utf-8", errors="replace")


def json_request(url, token="", timeout=DEFAULT_TIMEOUT):
    headers = {}
    if token:
        headers["Authori-zation"] = token
    status, content_type, payload = request("GET", url, headers=headers, timeout=timeout)
    try:
        parsed = json.loads(payload.decode("utf-8"))
    except Exception:
        parsed = None
    return status, content_type, payload, parsed


def json_post(url, data, token="", timeout=DEFAULT_TIMEOUT):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authori-zation"] = token
    body = json.dumps(data).encode("utf-8")
    status, content_type, payload = request("POST", url, body=body, headers=headers, timeout=timeout)
    try:
        parsed = json.loads(payload.decode("utf-8"))
    except Exception:
        parsed = None
    return status, content_type, payload, parsed


def multipart_upload(url, field_name, filename, content, token="", timeout=DEFAULT_TIMEOUT):
    boundary = "----crmeb-image-smoke-{}".format(int(time.time() * 1000))
    body = b"".join(
        [
            ("--{}\r\n".format(boundary)).encode("utf-8"),
            (
                'Content-Disposition: form-data; name="{}"; filename="{}"\r\n'
                "Content-Type: image/png\r\n\r\n"
            ).format(field_name, filename).encode("utf-8"),
            content,
            ("\r\n--{}--\r\n".format(boundary)).encode("utf-8"),
        ]
    )
    headers = {"Content-Type": "multipart/form-data; boundary={}".format(boundary)}
    if token:
        headers["Authori-zation"] = token
    return request("POST", url, body=body, headers=headers, timeout=timeout)


def body_preview(payload):
    return payload[:160].decode("utf-8", errors="replace").replace("\n", " ")


def is_private_url(value):
    try:
        parsed = urllib.parse.urlparse(value)
        return bool(PRIVATE_HOST_RE.match(parsed.hostname or ""))
    except Exception:
        return False


def crmeb_path(value):
    match = re.search(r"crmebimage/.*", value or "")
    if not match:
        return ""
    return "/" + match.group(0).lstrip("/")


def normalize_for_base(value, base_url):
    if not value:
        return ""
    value = value.strip()
    if value.startswith("/__image/"):
        return base_url.rstrip("/") + value
    if value.startswith("http://") or value.startswith("https://"):
        if "crmebimage/" in value and is_private_url(value):
            return base_url.rstrip("/") + crmeb_path(value)
        return value
    path = crmeb_path(value)
    if path:
        return base_url.rstrip("/") + path
    if value.startswith("/"):
        return base_url.rstrip("/") + value
    return value


def collect_images(value, source, results):
    if value is None:
        return
    if isinstance(value, str):
        for match in IMAGE_RE.finditer(value):
            results.append({"source": source, "raw": match.group(0)})
        return
    if isinstance(value, list):
        for item in value:
            collect_images(item, source, results)
        return
    if isinstance(value, dict):
        for key, item in value.items():
            collect_images(item, "{}.{}".format(source, key), results)


def response_data(parsed):
    if isinstance(parsed, dict):
        return parsed.get("data", parsed)
    return parsed


def add_api_images(name, url, token, images, checks, timeout):
    status, content_type, payload, parsed = json_request(url, token=token, timeout=timeout)
    ok = status == 200 and isinstance(parsed, dict) and str(parsed.get("code")) == "200"
    checks.append(
        {
            "name": name,
            "url": url,
            "ok": ok,
            "detail": "HTTP {} {}, body {}".format(status, content_type, body_preview(payload)),
        }
    )
    if ok:
        collect_images(response_data(parsed), name, images)


def image_payload_ok(content_type, payload):
    lowered = (content_type or "").lower()
    if "text/html" in lowered or payload.lstrip().startswith(b"<!DOCTYPE html") or payload.lstrip().startswith(b"<html"):
        return False
    if lowered.startswith("image/"):
        return True
    return (
        payload.startswith(b"\x89PNG")
        or payload.startswith(b"\xff\xd8\xff")
        or payload.startswith(b"GIF8")
        or payload.startswith(b"RIFF")
        or len(payload) > 64
    )


def raw_image_value_ok(raw):
    if not raw:
        return True
    scheme_count = raw.count("http://") + raw.count("https://")
    if scheme_count > 1:
        return False
    if (raw.startswith("http://") or raw.startswith("https://")) and is_private_url(raw):
        return False
    return not raw.startswith("undefined")


def check_raw_image_value(item):
    ok = raw_image_value_ok(item["raw"])
    return {
        "name": "{} raw image value".format(item["source"]),
        "url": item["raw"],
        "ok": ok,
        "detail": "API returned a private, duplicated, or undefined image path: {}".format(item["raw"]),
    }


def check_image_url(label, url, timeout):
    status, content_type, payload = request("GET", url, timeout=timeout)
    ok = status == 200 and image_payload_ok(content_type, payload)
    return {
        "name": label,
        "url": url,
        "ok": ok,
        "detail": "HTTP {} {}, {} bytes, body {}".format(status, content_type, len(payload), body_preview(payload)),
    }


def unique_images(images, max_images):
    seen = set()
    unique = []
    for item in images:
        key = item["raw"]
        if key in seen:
            continue
        seen.add(key)
        unique.append(item)
        if len(unique) >= max_images:
            break
    return unique


def run_upload_checks(args, checks, images):
    upload_targets = [
        {
            "name": "admin upload image",
            "url": args.admin_api.rstrip("/") + "/api/admin/upload/image?model=order&pid=0",
            "token": "",
        }
    ]
    if args.front_token:
        upload_targets.append(
            {
                "name": "front user upload image",
                "url": args.front_api.rstrip("/") + "/api/front/upload/image?model=order&pid=1",
                "token": args.front_token,
            }
        )

    for target in upload_targets:
        status, content_type, payload = multipart_upload(
            target["url"],
            "multipart",
            "smoke-payment-proof.png",
            PNG_1X1,
            token=target["token"],
            timeout=args.timeout,
        )
        try:
            parsed = json.loads(payload.decode("utf-8"))
        except Exception:
            parsed = None
        ok = status == 200 and isinstance(parsed, dict) and str(parsed.get("code")) == "200"
        checks.append(
            {
                "name": target["name"],
                "url": target["url"],
                "ok": ok,
                "detail": "HTTP {} {}, body {}".format(status, content_type, body_preview(payload)),
            }
        )
        if ok:
            uploaded = parsed.get("data", {}).get("url", "")
            if uploaded:
                images.append({"source": target["name"], "raw": uploaded})
                target["uploaded"] = uploaded


def run_offline_proof_submit_check(args, checks, images):
    if not args.front_token or not args.submit_proof_order_id:
        return

    upload_url = args.front_api.rstrip("/") + "/api/front/upload/image?model=order&pid=1"
    status, content_type, payload = multipart_upload(
        upload_url,
        "multipart",
        "smoke-payment-proof-submit.png",
        PNG_1X1,
        token=args.front_token,
        timeout=args.timeout,
    )
    try:
        parsed = json.loads(payload.decode("utf-8"))
    except Exception:
        parsed = None
    uploaded = parsed.get("data", {}).get("url", "") if isinstance(parsed, dict) else ""
    upload_ok = status == 200 and isinstance(parsed, dict) and str(parsed.get("code")) == "200" and uploaded
    checks.append(
        {
            "name": "front payment proof upload for submit",
            "url": upload_url,
            "ok": upload_ok,
            "detail": "HTTP {} {}, body {}".format(status, content_type, body_preview(payload)),
        }
    )
    if not upload_ok:
        return
    images.append({"source": "front payment proof upload for submit", "raw": uploaded})

    submit_url = args.front_api.rstrip("/") + "/api/front/pay/offline/proof"
    status, content_type, payload, parsed = json_post(
        submit_url,
        {
            "orderNo": args.submit_proof_order_id,
            "voucher": uploaded,
            "tradeNo": "smoke-image-submit",
            "remark": "image smoke submit",
        },
        token=args.front_token,
        timeout=args.timeout,
    )
    submit_ok = status == 200 and isinstance(parsed, dict) and str(parsed.get("code")) == "200"
    checks.append(
        {
            "name": "front submit payment proof",
            "url": submit_url,
            "ok": submit_ok,
            "detail": "HTTP {} {}, body {}".format(status, content_type, body_preview(payload)),
        }
    )
    if submit_ok:
        collect_images(response_data(parsed), "front submit payment proof", images)
        add_api_images(
            "front order detail payment proof after submit",
            args.front_api.rstrip("/") + "/api/front/order/detail/{}".format(
                urllib.parse.quote(args.submit_proof_order_id)
            ),
            args.front_token,
            images,
            checks,
            args.timeout,
        )
        if args.admin_token:
            add_api_images(
                "admin order detail payment proof after submit",
                args.admin_api.rstrip("/") + "/api/admin/store/order/info?orderNo={}".format(
                    urllib.parse.quote(args.submit_proof_order_id)
                ),
                args.admin_token,
                images,
                checks,
                args.timeout,
            )


def main():
    parser = argparse.ArgumentParser(description="CRMEB image display, upload, and thumbnail smoke tests")
    parser.add_argument("--admin-web", default="http://127.0.0.1:8080")
    parser.add_argument("--app-h5", default="http://127.0.0.1:8082")
    parser.add_argument("--admin-api", default="http://127.0.0.1:20400")
    parser.add_argument("--front-api", default="http://127.0.0.1:20410")
    parser.add_argument("--admin-token", default="")
    parser.add_argument("--front-token", default="")
    parser.add_argument("--order-id", default="", help="Optional user order number to verify order detail voucher image")
    parser.add_argument("--admin-order-id", default="", help="Optional admin order number to verify order detail voucher image")
    parser.add_argument("--submit-proof-order-id", default="", help="Optional order number used to upload and submit a fresh payment proof")
    parser.add_argument("--skip-upload", action="store_true")
    parser.add_argument("--max-images", type=int, default=120)
    parser.add_argument("--timeout", type=int, default=DEFAULT_TIMEOUT)
    args = parser.parse_args()

    admin_web = args.admin_web.rstrip("/")
    app_h5 = args.app_h5.rstrip("/")
    front_api = args.front_api.rstrip("/")
    admin_api = args.admin_api.rstrip("/")

    checks = []
    images = []

    public_sources = [
        ("front index images", front_api + "/api/front/index"),
        ("front category thumbnails", front_api + "/api/front/category"),
        ("front hot product images", front_api + "/api/front/product/hot?page=1&limit=20"),
        ("front product list images", front_api + "/api/front/products?page=1&limit=20"),
        ("front product model images", front_api + "/api/front/product/list?page=1&limit=20"),
    ]
    for name, url in public_sources:
        add_api_images(name, url, "", images, checks, args.timeout)

    if args.admin_token:
        add_api_images(
            "admin attachment thumbnails",
            admin_api + "/api/admin/system/attachment/list?pid=0&attType=jpg,jpeg,gif,png,bmp,PNG,JPG&page=1&limit=50",
            args.admin_token,
            images,
            checks,
            args.timeout,
        )
        add_api_images(
            "admin order list payment proof",
            admin_api + "/api/admin/store/order/list?page=1&limit=20&type=2&status=offlineReview",
            args.admin_token,
            images,
            checks,
            args.timeout,
        )
        if args.admin_order_id:
            add_api_images(
                "admin order detail payment proof",
                admin_api + "/api/admin/store/order/info?orderNo={}".format(urllib.parse.quote(args.admin_order_id)),
                args.admin_token,
                images,
                checks,
                args.timeout,
            )
        add_api_images(
            "admin product images",
            admin_api + "/api/admin/store/product/list?page=1&limit=50&type=2",
            args.admin_token,
            images,
            checks,
            args.timeout,
        )

    if args.front_token and args.order_id:
        add_api_images(
            "front order detail payment proof",
            front_api + "/api/front/order/detail/{}".format(urllib.parse.quote(args.order_id)),
            args.front_token,
            images,
            checks,
            args.timeout,
        )

    if not args.skip_upload:
        run_upload_checks(args, checks, images)
        run_offline_proof_submit_check(args, checks, images)

    for item in unique_images(images, args.max_images):
        checks.append(check_raw_image_value(item))
        raw = item["raw"]
        path = crmeb_path(raw)
        if (raw.startswith("http://") or raw.startswith("https://")) and not is_private_url(raw):
            checks.append(check_image_url(item["source"], raw, args.timeout))
        elif path:
            checks.append(check_image_url("{} via admin-web".format(item["source"]), admin_web + path, args.timeout))
            checks.append(check_image_url("{} via app-h5".format(item["source"]), app_h5 + path, args.timeout))
        else:
            checks.append(check_image_url(item["source"], normalize_for_base(raw, app_h5), args.timeout))

    failures = []
    for check in checks:
        prefix = "PASS" if check["ok"] else "FAIL"
        print("{} {} -> {}".format(prefix, check["name"], check["url"]))
        if not check["ok"]:
            print("  {}".format(check["detail"]))
            failures.append(check)

    if failures:
        print("\n{} image smoke check(s) failed.".format(len(failures)), file=sys.stderr)
        return 1

    print("\nAll image smoke checks passed. Checked {} API/upload steps and {} image URL(s).".format(
        len(checks) - len(unique_images(images, args.max_images)),
        len(unique_images(images, args.max_images)),
    ))
    return 0


if __name__ == "__main__":
    sys.exit(main())
