#!/usr/bin/env python3
import argparse
import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from decimal import Decimal


DEFAULT_TIMEOUT = 20
REQUIRED_PRODUCT_UNITS = 8
PNG_1X1 = (
    b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01"
    b"\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\nIDATx\x9cc\xf8\x0f"
    b"\x00\x01\x01\x01\x00\x18\xdd\x8d\xb0\x00\x00\x00\x00IEND\xaeB`\x82"
)


class SmokeFailure(Exception):
    pass


class PurchaseFlowSmoke:
    def __init__(self, args):
        self.args = args
        self.admin_api = args.admin_api.rstrip("/")
        self.front_api = args.front_api.rstrip("/")
        self.admin_token = ""
        self.front_token = ""
        self.checks = []

    def pass_check(self, name, detail=""):
        self.checks.append((True, name, detail))
        suffix = " -> {}".format(detail) if detail else ""
        print("PASS {}{}".format(name, suffix))

    def fail_check(self, name, detail):
        self.checks.append((False, name, detail))
        print("FAIL {} -> {}".format(name, detail))
        raise SmokeFailure("{}: {}".format(name, detail))

    def assert_true(self, name, condition, detail):
        if condition:
            self.pass_check(name, detail)
        else:
            self.fail_check(name, detail)

    def request(self, method, url, body=None, token="", headers=None):
        req_headers = {"User-Agent": "crmeb-purchase-flow-smoke/1.0"}
        if headers:
            req_headers.update(headers)
        if token:
            req_headers["Authori-zation"] = token
        req = urllib.request.Request(url, data=body, headers=req_headers, method=method)
        try:
            with urllib.request.urlopen(req, timeout=self.args.timeout) as resp:
                return resp.status, resp.headers.get("Content-Type", ""), resp.read()
        except urllib.error.HTTPError as exc:
            return exc.code, exc.headers.get("Content-Type", ""), exc.read()
        except Exception as exc:
            return 0, "", str(exc).encode("utf-8", errors="replace")

    def json_api(self, name, method, base_url, path, token="", data=None, params=None, expect_code=200):
        url = base_url + path
        if params:
            url += "?" + urllib.parse.urlencode(params, doseq=True)
        body = None
        headers = {}
        if data is not None:
            body = json.dumps(data).encode("utf-8")
            headers["Content-Type"] = "application/json"
        status, content_type, payload = self.request(method, url, body=body, token=token, headers=headers)
        try:
            parsed = json.loads(payload.decode("utf-8"))
        except Exception:
            self.fail_check(name, "HTTP {} {}, non-JSON body {}".format(status, content_type, self.preview(payload)))

        api_code = parsed.get("code", parsed.get("repCode"))
        ok = status == 200 and str(api_code) == str(expect_code)
        if not ok:
            self.fail_check(name, "HTTP {} {}, code {}, body {}".format(status, content_type, api_code, self.preview(payload)))
        self.pass_check(name, "HTTP 200 code {}".format(api_code))
        return parsed.get("data")

    def raw_json_post(self, name, url, data):
        body = json.dumps(data).encode("utf-8")
        status, content_type, payload = self.request("POST", url, body=body, headers={"Content-Type": "application/json"})
        try:
            parsed = json.loads(payload.decode("utf-8"))
        except Exception:
            self.fail_check(name, "HTTP {} {}, non-JSON body {}".format(status, content_type, self.preview(payload)))
        if status != 200 or str(parsed.get("code")) != "200":
            self.fail_check(name, "HTTP {} {}, body {}".format(status, content_type, self.preview(payload)))
        self.pass_check(name, "HTTP 200 code {}".format(parsed.get("code")))
        return parsed.get("data")

    def preview(self, payload):
        return payload[:240].decode("utf-8", errors="replace").replace("\n", " ")

    def login(self):
        admin_data = self.raw_json_post(
            "admin login",
            self.admin_api + "/api/admin/login",
            {"account": self.args.admin_account, "pwd": self.args.admin_password},
        )
        self.admin_token = admin_data.get("token", "") if isinstance(admin_data, dict) else ""
        self.assert_true("admin token returned", bool(self.admin_token), "token length {}".format(len(self.admin_token)))

        front_data = self.raw_json_post(
            "front login",
            self.front_api + "/api/front/login",
            {"account": self.args.front_account, "password": self.args.front_password},
        )
        self.front_token = front_data.get("token", "") if isinstance(front_data, dict) else ""
        self.assert_true("front token returned", bool(self.front_token), "token length {}".format(len(self.front_token)))

    def choose_product(self):
        products = self.json_api(
            "front product list",
            "GET",
            self.front_api,
            "/api/front/products",
            params={"page": 1, "limit": 50},
        )
        candidates = (products or {}).get("list") or []
        for product in candidates:
            product_id = product.get("id")
            if not product_id or int(product.get("stock") or 0) < REQUIRED_PRODUCT_UNITS:
                continue
            detail = self.json_api(
                "front product detail {}".format(product_id),
                "GET",
                self.front_api,
                "/api/front/product/detail/{}".format(product_id),
                params={"type": "normal"},
            )
            values = (detail or {}).get("productValue") or {}
            for sku_name, sku in values.items():
                stock = int(sku.get("stock") or 0)
                attr_id = sku.get("id")
                if attr_id and stock >= REQUIRED_PRODUCT_UNITS:
                    selected = {
                        "productId": int(product_id),
                        "attrValueId": int(attr_id),
                        "skuName": sku_name,
                        "stock": stock,
                        "price": str(sku.get("price") or product.get("price") or "0"),
                    }
                    self.pass_check(
                        "select purchasable sku",
                        "product {} attr {} stock {}".format(selected["productId"], selected["attrValueId"], stock),
                    )
                    return selected
        self.fail_check("select purchasable sku", "no active product SKU has stock >= {}".format(REQUIRED_PRODUCT_UNITS))

    def ensure_address(self):
        default_address = self.json_api(
            "front default address",
            "GET",
            self.front_api,
            "/api/front/address/default",
            token=self.front_token,
        )
        if isinstance(default_address, dict) and default_address.get("id"):
            self.pass_check("use existing default address", "id {}".format(default_address["id"]))
            return int(default_address["id"])

        address = self.json_api(
            "front create address",
            "POST",
            self.front_api,
            "/api/front/address/edit",
            token=self.front_token,
            data={
                "realName": "Smoke Tester",
                "phone": self.args.address_phone,
                "detail": "Purchase flow smoke test address",
                "isDefault": True,
                "address": {
                    "province": "北京市",
                    "city": "北京市",
                    "district": "东城区",
                    "cityId": 0,
                },
            },
        )
        self.assert_true("created address id returned", isinstance(address, dict) and address.get("id"), str(address))
        return int(address["id"])

    def upload_payment_proof(self, name):
        boundary = "----crmeb-purchase-flow-{}".format(int(time.time() * 1000))
        body = b"".join(
            [
                ("--{}\r\n".format(boundary)).encode("utf-8"),
                b'Content-Disposition: form-data; name="multipart"; filename="purchase-flow-proof.png"\r\n',
                b"Content-Type: image/png\r\n\r\n",
                PNG_1X1,
                ("\r\n--{}--\r\n".format(boundary)).encode("utf-8"),
            ]
        )
        status, content_type, payload = self.request(
            "POST",
            self.front_api + "/api/front/upload/image?model=order&pid=1",
            body=body,
            token=self.front_token,
            headers={"Content-Type": "multipart/form-data; boundary={}".format(boundary)},
        )
        try:
            parsed = json.loads(payload.decode("utf-8"))
        except Exception:
            self.fail_check(name, "HTTP {} {}, non-JSON body {}".format(status, content_type, self.preview(payload)))
        if status != 200 or str(parsed.get("code")) != "200":
            self.fail_check(name, "HTTP {} {}, body {}".format(status, content_type, self.preview(payload)))
        url = ((parsed.get("data") or {}).get("url") or "").strip()
        self.assert_true(name, url.startswith("/crmebimage/") or url.startswith("crmebimage/"), url)
        return url

    def cart_smoke(self, sku):
        data = self.json_api(
            "front cart add",
            "POST",
            self.front_api,
            "/api/front/cart/save",
            token=self.front_token,
            data={
                "productId": sku["productId"],
                "productAttrUnique": str(sku["attrValueId"]),
                "cartNum": 1,
            },
        )
        cart_id = (data or {}).get("cartId")
        self.assert_true("cart id returned", bool(cart_id), str(data))
        self.json_api(
            "front cart list",
            "GET",
            self.front_api,
            "/api/front/cart/list",
            token=self.front_token,
            params={"isValid": "true", "page": 1, "limit": 10},
        )
        self.json_api(
            "front cart count",
            "GET",
            self.front_api,
            "/api/front/cart/count",
            token=self.front_token,
            params={"type": "total", "numType": "true"},
        )
        self.json_api(
            "front cart delete",
            "POST",
            self.front_api,
            "/api/front/cart/delete",
            token=self.front_token,
            params={"ids": [cart_id]},
        )

    def create_order(self, sku, address_id, label):
        pre = self.json_api(
            "{} pre order".format(label),
            "POST",
            self.front_api,
            "/api/front/order/pre/order",
            token=self.front_token,
            data={
                "preOrderType": "buyNow",
                "orderDetails": [
                    {
                        "productId": sku["productId"],
                        "attrValueId": sku["attrValueId"],
                        "productNum": 1,
                    }
                ],
            },
        )
        pre_order_no = (pre or {}).get("preOrderNo")
        self.assert_true("{} pre order number returned".format(label), bool(pre_order_no), str(pre))

        self.json_api(
            "{} load pre order".format(label),
            "GET",
            self.front_api,
            "/api/front/order/load/pre/{}".format(urllib.parse.quote(pre_order_no)),
            token=self.front_token,
        )
        self.json_api(
            "{} computed price".format(label),
            "POST",
            self.front_api,
            "/api/front/order/computed/price",
            token=self.front_token,
            data={
                "preOrderNo": pre_order_no,
                "addressId": address_id,
                "couponId": 0,
                "shippingType": 1,
                "useIntegral": False,
            },
        )
        created = self.json_api(
            "{} create order".format(label),
            "POST",
            self.front_api,
            "/api/front/order/create",
            token=self.front_token,
            data={
                "preOrderNo": pre_order_no,
                "shippingType": 1,
                "addressId": address_id,
                "couponId": 0,
                "useIntegral": False,
                "mark": "purchase flow smoke {}".format(label),
            },
        )
        order_no = (created or {}).get("orderNo")
        self.assert_true("{} order number returned".format(label), bool(order_no), str(created))
        detail = self.front_order_detail(order_no, "{} front detail after create".format(label))
        self.assert_true("{} starts unpaid".format(label), detail.get("paid") is False, "paid={}".format(detail.get("paid")))
        return order_no

    def front_order_detail(self, order_no, name=None):
        return self.json_api(
            name or "front order detail {}".format(order_no),
            "GET",
            self.front_api,
            "/api/front/order/detail/{}".format(urllib.parse.quote(order_no)),
            token=self.front_token,
        )

    def admin_order_detail(self, order_no, name=None):
        return self.json_api(
            name or "admin order detail {}".format(order_no),
            "GET",
            self.admin_api,
            "/api/admin/store/order/info",
            token=self.admin_token,
            params={"orderNo": order_no},
        )

    def select_offline_payment(self, order_no, label):
        config = self.json_api(
            "{} front pay config".format(label),
            "GET",
            self.front_api,
            "/api/front/pay/get/config",
            token=self.front_token,
        )
        self.assert_true(
            "{} offline pay config open".format(label),
            (config or {}).get("offlinePayStatus") in (True, 1, "1", "true", "True"),
            "offlinePayStatus={}".format((config or {}).get("offlinePayStatus")),
        )
        result = self.json_api(
            "{} select offline payment".format(label),
            "POST",
            self.front_api,
            "/api/front/pay/payment",
            token=self.front_token,
            data={
                "orderNo": order_no,
                "uni": order_no,
                "payType": "offline",
                "payChannel": "offline",
                "from": "h5",
            },
        )
        self.assert_true(
            "{} offline payment selected".format(label),
            (result or {}).get("payType") == "offline",
            "payType={}".format((result or {}).get("payType")),
        )

    def submit_payment_proof(self, order_no, label):
        voucher = self.upload_payment_proof("{} upload payment proof".format(label))
        result = self.json_api(
            "{} submit payment proof".format(label),
            "POST",
            self.front_api,
            "/api/front/pay/offline/proof",
            token=self.front_token,
            data={
                "orderNo": order_no,
                "voucher": voucher,
                "tradeNo": "smoke-{}".format(int(time.time() * 1000)),
                "remark": label,
            },
        )
        self.assert_true(
            "{} proof pending".format(label),
            (result or {}).get("offlinePayStatus") == 1,
            "offlinePayStatus={}".format((result or {}).get("offlinePayStatus")),
        )
        detail = self.admin_order_detail(order_no, "{} admin detail after proof".format(label))
        self.assert_true(
            "{} admin sees voucher".format(label),
            bool(detail.get("offlinePayVoucher")),
            "voucher={}".format(detail.get("offlinePayVoucher")),
        )

    def audit_payment(self, order_no, approved, label):
        data = {"orderNo": order_no, "approved": approved}
        if not approved:
            data["reason"] = "purchase flow smoke rejection"
        self.json_api(
            "{} offline audit {}".format(label, "approve" if approved else "reject"),
            "POST",
            self.admin_api,
            "/api/admin/store/order/offline/audit",
            token=self.admin_token,
            data=data,
        )
        detail = self.admin_order_detail(order_no, "{} admin detail after audit".format(label))
        expected_status = 2 if approved else 3
        self.assert_true(
            "{} offline status after audit".format(label),
            detail.get("offlinePayStatus") == expected_status,
            "offlinePayStatus={}".format(detail.get("offlinePayStatus")),
        )
        paid_detail = self.front_order_detail(order_no, "{} front detail after audit".format(label))
        self.assert_true(
            "{} paid flag after audit".format(label),
            paid_detail.get("paid") is approved,
            "paid={}".format(paid_detail.get("paid")),
        )

    def ship_order(self, order_no, label):
        self.json_api(
            "{} admin ship fictitious".format(label),
            "POST",
            self.admin_api,
            "/api/admin/store/order/send",
            token=self.admin_token,
            data={
                "orderNo": order_no,
                "deliveryType": "fictitious",
            },
        )
        detail = self.admin_order_detail(order_no, "{} admin detail after ship".format(label))
        self.assert_true("{} shipped status".format(label), detail.get("status") == 1, "status={}".format(detail.get("status")))
        self.front_logistics_for_virtual_order(order_no, label)

    def front_logistics_for_virtual_order(self, order_no, label):
        url = self.front_api + "/api/front/order/express/{}".format(urllib.parse.quote(order_no))
        status, content_type, payload = self.request("GET", url, token=self.front_token)
        try:
            parsed = json.loads(payload.decode("utf-8"))
        except Exception:
            self.fail_check(
                "{} front logistics virtual order".format(label),
                "HTTP {} {}, non-JSON body {}".format(status, content_type, self.preview(payload)),
            )
        api_code = parsed.get("code")
        message = parsed.get("message") or ""
        ok = status == 200 and (
            str(api_code) == "200" or (str(api_code) == "500" and "快递订单号" in message)
        )
        self.assert_true(
            "{} front logistics virtual order".format(label),
            ok,
            "HTTP {} code {} message {}".format(status, api_code, message),
        )

    def take_order(self, order_no, label):
        detail = self.front_order_detail(order_no, "{} front detail before take".format(label))
        order_id = detail.get("id")
        self.assert_true("{} front detail id before take".format(label), bool(order_id), "id={}".format(order_id))
        self.json_api(
            "{} front take order".format(label),
            "POST",
            self.front_api,
            "/api/front/order/take",
            token=self.front_token,
            params={"id": order_id},
        )
        detail = self.front_order_detail(order_no, "{} front detail after take".format(label))
        self.assert_true("{} received status".format(label), detail.get("status") == 2, "status={}".format(detail.get("status")))

    def apply_refund(self, order_no, label):
        apply_info = self.json_api(
            "{} front apply refund info".format(label),
            "GET",
            self.front_api,
            "/api/front/order/apply/refund/{}".format(urllib.parse.quote(order_no)),
            token=self.front_token,
        )
        order_id = (apply_info or {}).get("id")
        self.assert_true("{} apply refund info id".format(label), bool(order_id), "id={}".format(order_id))
        self.json_api(
            "{} front refund reasons".format(label),
            "GET",
            self.front_api,
            "/api/front/order/refund/reason",
            token=self.front_token,
        )
        self.json_api(
            "{} front apply refund".format(label),
            "POST",
            self.front_api,
            "/api/front/order/refund",
            token=self.front_token,
            data={
                "id": order_id,
                "uni": order_no,
                "text": "purchase flow smoke refund",
                "refund_reason_wap_explain": label,
                "refund_reason_wap_img": "",
            },
        )
        detail = self.admin_order_detail(order_no, "{} admin detail after refund apply".format(label))
        self.assert_true("{} refund requested".format(label), detail.get("refundStatus") == 1, "refundStatus={}".format(detail.get("refundStatus")))
        return detail

    def approve_refund(self, order_no, label):
        detail = self.admin_order_detail(order_no, "{} admin detail before refund approve".format(label))
        amount = str(Decimal(str(detail.get("payPrice") or "0")).quantize(Decimal("0.01")))
        self.assert_true("{} refund amount positive".format(label), Decimal(amount) > Decimal("0"), "amount={}".format(amount))
        self.json_api(
            "{} admin refund approve".format(label),
            "GET",
            self.admin_api,
            "/api/admin/store/order/refund",
            token=self.admin_token,
            params={"orderNo": order_no, "amount": amount},
        )

        def final_refunded():
            current = self.admin_order_detail(order_no, "{} poll refunded".format(label))
            return current.get("refundStatus") == 2, current

        current = self.poll("{} refund final status".format(label), final_refunded, self.args.refund_timeout)
        self.assert_true("{} refund completed".format(label), current.get("refundStatus") == 2, "refundStatus={}".format(current.get("refundStatus")))

    def refuse_refund(self, order_no, label):
        reason = "purchase flow smoke refuse"
        self.json_api(
            "{} admin refund refuse".format(label),
            "GET",
            self.admin_api,
            "/api/admin/store/order/refund/refuse",
            token=self.admin_token,
            params={"orderNo": order_no, "reason": reason},
        )
        detail = self.admin_order_detail(order_no, "{} admin detail after refund refuse".format(label))
        self.assert_true("{} refund reset after refuse".format(label), detail.get("refundStatus") == 0, "refundStatus={}".format(detail.get("refundStatus")))
        self.assert_true("{} refund refuse reason saved".format(label), detail.get("refundReason") == reason, "refundReason={}".format(detail.get("refundReason")))

    def cancel_order(self, order_no, label):
        detail = self.front_order_detail(order_no, "{} front detail before cancel".format(label))
        order_id = detail.get("id")
        self.assert_true("{} cancel id returned".format(label), bool(order_id), "id={}".format(order_id))
        self.json_api(
            "{} front cancel order".format(label),
            "POST",
            self.front_api,
            "/api/front/order/cancel",
            token=self.front_token,
            params={"id": order_id},
        )

    def admin_status_smoke(self):
        self.json_api(
            "admin order status num",
            "GET",
            self.admin_api,
            "/api/admin/store/order/status/num",
            token=self.admin_token,
            params={"type": 2},
        )
        self.json_api(
            "admin order list data",
            "GET",
            self.admin_api,
            "/api/admin/store/order/list/data",
            token=self.admin_token,
        )

    def front_order_smoke(self):
        self.json_api("front order data", "GET", self.front_api, "/api/front/order/data", token=self.front_token)
        for order_type in [0, 1, 2, 3, 4, -3]:
            self.json_api(
                "front order list type {}".format(order_type),
                "GET",
                self.front_api,
                "/api/front/order/list",
                token=self.front_token,
                params={"type": order_type, "page": 1, "limit": 5},
            )

    def poll(self, name, callback, timeout):
        deadline = time.time() + timeout
        last = None
        while True:
            ok, value = callback()
            last = value
            if ok:
                self.pass_check(name, "condition met")
                return value
            if time.time() >= deadline:
                self.fail_check(name, "timed out after {}s; last={}".format(timeout, last))
            time.sleep(self.args.poll_interval)

    def paid_order(self, sku, address_id, label):
        order_no = self.create_order(sku, address_id, label)
        self.select_offline_payment(order_no, label)
        self.submit_payment_proof(order_no, label)
        self.audit_payment(order_no, True, label)
        return order_no

    def run(self):
        self.login()
        sku = self.choose_product()
        address_id = self.ensure_address()
        self.cart_smoke(sku)
        self.admin_status_smoke()
        self.front_order_smoke()

        approve_order = self.paid_order(sku, address_id, "approved flow")
        self.ship_order(approve_order, "approved flow")
        self.take_order(approve_order, "approved flow")
        self.apply_refund(approve_order, "approved flow")
        self.approve_refund(approve_order, "approved flow")

        refuse_order = self.paid_order(sku, address_id, "refund refuse flow")
        self.apply_refund(refuse_order, "refund refuse flow")
        self.refuse_refund(refuse_order, "refund refuse flow")

        reject_order = self.create_order(sku, address_id, "offline reject flow")
        self.select_offline_payment(reject_order, "offline reject flow")
        self.submit_payment_proof(reject_order, "offline reject flow")
        self.audit_payment(reject_order, False, "offline reject flow")

        cancel_order = self.create_order(sku, address_id, "cancel flow")
        self.cancel_order(cancel_order, "cancel flow")

        self.front_order_smoke()
        self.admin_status_smoke()

        failed = [item for item in self.checks if not item[0]]
        if failed:
            print("\n{} purchase flow smoke check(s) failed.".format(len(failed)), file=sys.stderr)
            return 1
        print("\nAll purchase flow smoke checks passed. Checked {} step(s).".format(len(self.checks)))
        return 0


def main():
    parser = argparse.ArgumentParser(description="CRMEB end-to-end purchase flow smoke tests")
    parser.add_argument("--admin-api", default="http://127.0.0.1:20400")
    parser.add_argument("--front-api", default="http://127.0.0.1:20410")
    parser.add_argument("--admin-account", default="admin")
    parser.add_argument("--admin-password", default="123456")
    parser.add_argument("--front-account", default="18800001001")
    parser.add_argument("--front-password", default="Test123456")
    parser.add_argument("--address-phone", default="18800001001")
    parser.add_argument("--timeout", type=int, default=DEFAULT_TIMEOUT)
    parser.add_argument("--refund-timeout", type=int, default=90)
    parser.add_argument("--poll-interval", type=int, default=5)
    args = parser.parse_args()
    try:
        return PurchaseFlowSmoke(args).run()
    except SmokeFailure as exc:
        print("\nPurchase flow smoke failed: {}".format(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
