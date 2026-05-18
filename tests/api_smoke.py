#!/usr/bin/env python3
import argparse
import json
import sys
import urllib.error
import urllib.request


DEFAULT_TIMEOUT = 15


def request(method, url, body=None, timeout=DEFAULT_TIMEOUT):
    data = None
    headers = {"User-Agent": "crmeb-api-smoke/1.0"}
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"

    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            payload = resp.read()
            return resp.status, resp.headers.get("Content-Type", ""), payload
    except urllib.error.HTTPError as exc:
        return exc.code, exc.headers.get("Content-Type", ""), exc.read()


def body_text(payload):
    return payload[:300].decode("utf-8", errors="replace").replace("\n", " ")


def expect_http_status(status, expected):
    return status in expected


def expect_json_code(payload, expected_codes):
    try:
        parsed = json.loads(payload.decode("utf-8"))
    except Exception:
        return False, "response is not JSON"

    code = parsed.get("code", parsed.get("repCode"))
    if code in expected_codes or str(code) in {str(item) for item in expected_codes}:
        return True, ""
    return False, "unexpected JSON code {!r}".format(code)


def run_check(check, timeout):
    method = check.get("method", "GET")
    status, content_type, payload = request(method, check["url"], check.get("body"), timeout)

    errors = []
    if not expect_http_status(status, set(check["status"])):
        errors.append("HTTP {} not in {}".format(status, check["status"]))

    if check.get("json_code") is not None:
        ok, detail = expect_json_code(payload, set(check["json_code"]))
        if not ok:
            errors.append(detail)

    if check.get("contains") and check["contains"] not in body_text(payload):
        errors.append("body does not contain {!r}".format(check["contains"]))

    return {
        "name": check["name"],
        "method": method,
        "url": check["url"],
        "status": status,
        "content_type": content_type,
        "body": body_text(payload),
        "errors": errors,
    }


def build_checks(args):
    admin_web = args.admin_web.rstrip("/")
    admin_api = args.admin_api.rstrip("/")
    front_api = args.front_api.rstrip("/")

    return [
        {
            "name": "admin web serves the SPA shell",
            "url": admin_web + "/",
            "status": [200],
            "contains": "<!DOCTYPE html>",
        },
        {
            "name": "admin web proxies root captcha API",
            "method": "POST",
            "url": admin_web + "/captcha/get",
            "body": {},
            "status": [200],
            "json_code": ["0011", "0000", 200],
        },
        {
            "name": "admin web preserves /api/ when proxying admin login",
            "url": admin_web + "/api/admin/login",
            "status": [200, 405],
            "json_code": [500, 405],
        },
        {
            "name": "admin web proxies protected APIs to backend",
            "url": admin_web + "/api/v2/api-docs",
            "status": [200, 401],
            "json_code": [401],
        },
        {
            "name": "admin API exposes Knife4j document page",
            "url": admin_api + "/doc.html",
            "status": [200],
            "contains": "<html",
        },
        {
            "name": "admin API login route exists",
            "url": admin_api + "/api/admin/login",
            "status": [200, 405],
            "json_code": [500, 405],
        },
        {
            "name": "admin API root captcha route exists",
            "method": "POST",
            "url": admin_api + "/captcha/get",
            "body": {},
            "status": [200],
            "json_code": ["0011", "0000", 200],
        },
        {
            "name": "front API public domain config works",
            "url": front_api + "/api/public/config/get/front/domain",
            "status": [200],
            "json_code": [200],
        },
        {
            "name": "front API category list works",
            "url": front_api + "/api/front/category",
            "status": [200],
            "json_code": [200],
        },
        {
            "name": "front API product hot endpoint is reachable",
            "url": front_api + "/api/front/product/hot",
            "status": [200],
            "json_code": [200, 500],
        },
    ]


def main():
    parser = argparse.ArgumentParser(description="CRMEB Docker deployment API smoke tests")
    parser.add_argument("--admin-web", default="http://127.0.0.1:8080")
    parser.add_argument("--admin-api", default="http://127.0.0.1:20400")
    parser.add_argument("--front-api", default="http://127.0.0.1:20410")
    parser.add_argument("--timeout", type=int, default=DEFAULT_TIMEOUT)
    args = parser.parse_args()

    failures = []
    for check in build_checks(args):
        result = run_check(check, args.timeout)
        prefix = "PASS" if not result["errors"] else "FAIL"
        print("{} {} {} -> {} {}".format(prefix, result["method"], result["url"], result["status"], result["content_type"]))
        if result["errors"]:
            print("  {}".format("; ".join(result["errors"])))
            print("  body: {}".format(result["body"]))
            failures.append(result)

    if failures:
        print("\n{} API smoke check(s) failed.".format(len(failures)), file=sys.stderr)
        return 1

    print("\nAll API smoke checks passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
