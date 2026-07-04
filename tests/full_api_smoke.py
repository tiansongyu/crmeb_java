#!/usr/bin/env python3
import argparse
import json
import pathlib
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request


DEFAULT_TIMEOUT = 15

SAFE_POST_CASES = [
    {
        "side": "front",
        "module": "提货点",
        "method": "POST",
        "path": "/api/front/store/list",
        "params": {"latitude": "30.2741", "longitude": "120.1551", "page": 1, "limit": 5},
        "token": "front",
    },
    {
        "side": "front",
        "module": "二维码服务",
        "method": "POST",
        "path": "/api/front/qrcode/str2base64",
        "params": {"text": "crmeb-smoke", "width": 120, "height": 120},
        "token": "front",
    },
    {
        "side": "front",
        "module": "安全验证控制器",
        "method": "POST",
        "path": "/api/public/safety/get",
        "json": {"captchaType": "blockPuzzle", "clientUid": "crmeb-smoke", "ts": 0},
    },
    {
        "side": "admin",
        "module": "用户积分管理",
        "method": "POST",
        "path": "/api/admin/user/integral/list",
        "json": {},
        "params": {"page": 1, "limit": 5},
        "token": "admin",
    },
    {
        "side": "admin",
        "module": "设置 -- 提货点 -- 核销订单",
        "method": "POST",
        "path": "/api/admin/system/store/order/list",
        "params": {"page": 1, "limit": 5},
        "token": "admin",
    },
]

EXTERNAL_OR_MUTATING_MODULES = {
    ("admin", "支付回调"),
    ("admin", "微信 -- 消息模版"),
    ("admin", "微信开放平台 -- 素材"),
    ("admin", "微信开放平台 -- 小程序回调"),
}

SKIP_SEGMENTS = {
    "add",
    "audit",
    "balance",
    "binding",
    "bindspread",
    "callback",
    "cancel",
    "cash",
    "clean",
    "clear",
    "completely",
    "create",
    "del",
    "delete",
    "edit",
    "logout",
    "payment",
    "receive",
    "recovery",
    "refund",
    "remove",
    "reset",
    "save",
    "send",
    "set",
    "setdefault",
    "start",
    "status",
    "stop",
    "suspend",
    "sync",
    "take",
    "trig",
    "update",
    "writeconfirm",
    "writeupdate",
    "restore",
    "putonshell",
    "offshell",
}

SKIP_PATH_REASONS = {
    "/api/front/pay/queryPayResult": "requires a gateway payment order; offline payment is covered by purchase smoke",
    "/api/front/user/sign/integral": "mutates daily sign-in state",
    "/api/front/wechat/config": "requires valid WeChat app configuration",
    "/api/admin/express/template": "requires OnePass logistics configuration",
    "/api/admin/pass/shipment/express": "requires OnePass logistics configuration",
    "/api/admin/sms/temps": "requires SMS provider permission/configuration",
    "/api/admin/statistics/user/overview/list": "current admin role does not expose this permission",
    "/api/admin/store/order/getLogisticsInfo": "requires logistics tracking provider configuration",
    "/api/admin/system/config/get/auth/host": "requires license/auth host configuration",
    "/api/admin/user/operate/founds": "mutates user funds/integral state",
    "/api/admin/wechat/config": "requires valid WeChat app configuration",
    "/api/admin/wechat/menu/public/get": "requires valid WeChat app configuration",
    "/api/front/bargain/detail/{id}": "current seed bargain rows do not have bargain attr values",
}

REQUIRED_PARAMS = {
    "/api/admin/system/store/info": ("id",),
    "/api/admin/system/store/staff/info": ("id",),
    "/api/admin/user/infobycondition": ("userId", "type"),
}


def request(method, url, body=None, token="", timeout=DEFAULT_TIMEOUT):
    headers = {"User-Agent": "crmeb-full-api-smoke/1.0"}
    data = None
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authori-zation"] = token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.headers.get("Content-Type", ""), resp.read()
    except urllib.error.HTTPError as exc:
        return exc.code, exc.headers.get("Content-Type", ""), exc.read()
    except Exception as exc:
        return 0, "", str(exc).encode("utf-8", errors="replace")


def parse_json(payload):
    try:
        return json.loads(payload.decode("utf-8"))
    except Exception:
        return None


def preview(payload):
    return payload[:260].decode("utf-8", errors="replace").replace("\n", " ")


def api_ok(status, payload):
    parsed = parse_json(payload)
    if status != 200 or not isinstance(parsed, dict):
        return False, parsed
    code = parsed.get("code", parsed.get("repCode"))
    return str(code) in {"200", "0000"}, parsed


def response_data(parsed):
    if isinstance(parsed, dict):
        return parsed.get("data")
    return None


def join_url(base, path, params=None):
    url = base.rstrip("/") + "/" + path.lstrip("/")
    if params:
        url += "?" + urllib.parse.urlencode(params)
    return url


def json_call(base, method, path, token="", params=None, body=None, timeout=DEFAULT_TIMEOUT):
    return request(method, join_url(base, path, params), body=body, token=token, timeout=timeout)


def login(base, path, body, timeout):
    status, content_type, payload = request(
        "POST", base.rstrip("/") + path, body=body, timeout=timeout
    )
    ok, parsed = api_ok(status, payload)
    if not ok:
        raise RuntimeError(
            "login failed {} {} {}".format(status, content_type, preview(payload))
        )
    token = (response_data(parsed) or {}).get("token", "")
    if not token:
        raise RuntimeError("login response did not include token")
    return token


def first_list(data):
    if isinstance(data, dict):
        for key in ("list", "records", "data"):
            value = data.get(key)
            if isinstance(value, list):
                return value
    if isinstance(data, list):
        return data
    return []


def get_nested_id(item, *keys):
    if not isinstance(item, dict):
        return None
    for key in keys:
        value = item.get(key)
        if value:
            return value
    return None


class FullApiSmoke:
    def __init__(self, args):
        self.args = args
        self.admin_token = ""
        self.front_token = ""
        self.context = {
            "product_id": "92",
            "attr_id": "1187",
            "category_id": "0",
            "article_cid": "0",
            "article_id": "1",
            "address_id": "3",
            "order_no": "",
            "seckill_time_id": "0",
            "seckill_id": "0",
            "combination_id": "0",
            "bargain_id": "0",
            "user_id": "",
            "role_id": "",
            "attachment_id": "",
            "page_diy_id": "",
            "system_group_id": "",
            "system_group_data_id": "",
            "store_id": "",
            "shipping_template_id": "",
            "product_rule_id": "",
            "express_id": "",
            "admin_id": "",
            "city_id": "",
            "form_temp_id": "",
            "staff_id": "",
            "user_group_id": "",
            "user_tag_id": "",
            "seckill_manger_id": "",
            "notification_id": "",
        }
        self.results = []
        self.skips = []

    def run(self):
        self.login()
        self.discover_context()
        cases = self.build_cases()
        for case in cases:
            self.run_case(case)
        self.print_summary(cases)

    def login(self):
        self.admin_token = login(
            self.args.admin_api,
            "/api/admin/login",
            {"account": self.args.admin_account, "pwd": self.args.admin_password},
            self.args.timeout,
        )
        self.front_token = login(
            self.args.front_api,
            "/api/front/login",
            {"account": self.args.front_account, "password": self.args.front_password},
            self.args.timeout,
        )

    def call(self, side, method, path, token_kind="", params=None, body=None):
        base = self.args.admin_api if side == "admin" else self.args.front_api
        token = ""
        if token_kind == "admin":
            token = self.admin_token
        elif token_kind == "front":
            token = self.front_token
        status, content_type, payload = json_call(
            base,
            method,
            path,
            token=token,
            params=params,
            body=body,
            timeout=self.args.timeout,
        )
        ok, parsed = api_ok(status, payload)
        return ok, parsed, status, content_type, payload

    def discover_context(self):
        ok, parsed, *_ = self.call(
            "front", "GET", "/api/front/products", params={"page": 1, "limit": 50}
        )
        if ok:
            products = first_list(response_data(parsed))
            if products:
                product = products[0]
                self.context["product_id"] = str(product.get("id") or self.context["product_id"])
                detail_ok, detail, *_ = self.call(
                    "front", "GET", "/api/front/product/detail/{}".format(self.context["product_id"])
                )
                if detail_ok:
                    data = response_data(detail) or {}
                    attr = first_list(data.get("productValue") or data.get("attrValue") or [])
                    if attr:
                        self.context["attr_id"] = str(
                            get_nested_id(attr[0], "id", "attrValueId") or self.context["attr_id"]
                        )

        ok, parsed, *_ = self.call("front", "GET", "/api/front/category")
        if ok:
            categories = first_list(response_data(parsed))
            if categories:
                self.context["category_id"] = str(categories[0].get("id") or "0")

        ok, parsed, *_ = self.call("front", "GET", "/api/front/article/category/list")
        if ok:
            categories = first_list(response_data(parsed))
            if categories:
                self.context["article_cid"] = str(categories[0].get("id") or "0")

        ok, parsed, *_ = self.call("front", "GET", "/api/front/article/hot/list")
        if ok:
            articles = first_list(response_data(parsed))
            if articles:
                self.context["article_id"] = str(articles[0].get("id") or "1")

        ok, parsed, *_ = self.call("front", "GET", "/api/front/seckill/header")
        if ok:
            headers = first_list(response_data(parsed))
            if headers:
                self.context["seckill_time_id"] = str(headers[0].get("id") or "0")

        ok, parsed, *_ = self.call(
            "front",
            "GET",
            "/api/front/seckill/list/{}".format(self.context["seckill_time_id"]),
            params={"page": 1, "limit": 5},
        )
        if ok:
            items = first_list(response_data(parsed))
            if items:
                self.context["seckill_id"] = str(items[0].get("id") or "0")

        for key, path in (
            ("combination_id", "/api/front/combination/list"),
            ("bargain_id", "/api/front/bargain/list"),
        ):
            ok, parsed, *_ = self.call("front", "GET", path, params={"page": 1, "limit": 5})
            if ok:
                items = first_list(response_data(parsed))
                if items:
                    self.context[key] = str(items[0].get("id") or "0")

        ok, parsed, *_ = self.call("front", "GET", "/api/front/address/default", token_kind="front")
        if ok and isinstance(response_data(parsed), dict):
            self.context["address_id"] = str(response_data(parsed).get("id") or self.context["address_id"])

        ok, parsed, *_ = self.call(
            "front",
            "GET",
            "/api/front/order/list",
            token_kind="front",
            params={"type": 0, "page": 1, "limit": 5},
        )
        if ok:
            orders = first_list(response_data(parsed))
            if orders:
                self.context["order_no"] = str(orders[0].get("orderId") or orders[0].get("orderNo") or "")

        admin_lists = [
            ("user_id", "/api/admin/user/list", "uid"),
            ("role_id", "/api/admin/system/role/list", "id"),
            ("attachment_id", "/api/admin/system/attachment/list", "attId"),
            ("page_diy_id", "/api/admin/pagediy/list", "id"),
            ("system_group_id", "/api/admin/system/group/list", "id"),
            ("system_group_data_id", "/api/admin/system/group/data/list", "id"),
            ("store_id", "/api/admin/system/store/list", "id"),
            ("shipping_template_id", "/api/admin/express/shipping/templates/list", "id"),
            ("product_rule_id", "/api/admin/store/product/rule/list", "id"),
            ("express_id", "/api/admin/express/list", "id"),
            ("admin_id", "/api/admin/system/admin/list", "id"),
            ("city_id", "/api/admin/system/city/list", "id"),
            ("form_temp_id", "/api/admin/system/form/temp/list", "id"),
            ("staff_id", "/api/admin/system/store/staff/list", "id"),
            ("user_group_id", "/api/admin/user/group/list", "id"),
            ("user_tag_id", "/api/admin/user/tag/list", "id"),
            ("seckill_manger_id", "/api/admin/store/seckill/manger/list", "id"),
            ("notification_id", "/api/admin/system/notification/list", "id"),
        ]
        for key, path, id_key in admin_lists:
            params = self.default_params(path)
            ok, parsed, *_ = self.call("admin", "GET", path, token_kind="admin", params=params)
            if ok:
                items = first_list(response_data(parsed))
                if items:
                    self.context[key] = str(items[0].get(id_key) or items[0].get("id") or "")

    def build_cases(self):
        cases = []
        cases.extend(self.discover_source_cases("front"))
        cases.extend(self.discover_source_cases("admin"))
        cases.extend(SAFE_POST_CASES)
        return cases

    def discover_source_cases(self, side):
        roots = []
        if side == "front":
            roots = [
                pathlib.Path("crmeb/crmeb-front/src/main/java/com/zbkj/front/controller"),
                pathlib.Path("crmeb/crmeb-front/src/main/java/com/zbkj/front/pub"),
            ]
        else:
            roots = [pathlib.Path("crmeb/crmeb-admin/src/main/java/com/zbkj/admin/controller")]

        cases = []
        covered_modules = set()
        for root in roots:
            for source in sorted(root.glob("*.java")):
                text = source.read_text(encoding="utf-8", errors="ignore")
                text = re.sub(r"//.*", "", text)
                base_match = re.search(r"@RequestMapping\(\s*(?:value\s*=\s*)?\"([^\"]+)\"", text)
                tag_match = re.search(r"@Api\(tags\s*=\s*\"([^\"]+)\"", text)
                if not base_match:
                    continue
                module = tag_match.group(1) if tag_match else source.stem
                if (side, module) in EXTERNAL_OR_MUTATING_MODULES:
                    self.skips.append((side, module, "external callback/sync/upload endpoint"))
                    continue
                base = "/" + base_match.group(1).strip("/")
                module_cases = []
                for op in re.finditer(
                    r"@RequestMapping\(value\s*=\s*\"([^\"]+)\"\s*,\s*method\s*=\s*RequestMethod\.([A-Z]+)\)",
                    text,
                ):
                    path = normalize_path(base + "/" + op.group(1).strip("/"))
                    method = op.group(2)
                    if method != "GET":
                        continue
                    skip_reason = self.endpoint_skip_reason(path)
                    if skip_reason:
                        self.skips.append((side, module, "{}: {}".format(path, skip_reason)))
                        continue
                    if not self.is_safe_get(path):
                        continue
                    built = self.build_case(side, module, method, path)
                    if built:
                        module_cases.append(built)
                    else:
                        self.skips.append((side, module, "missing sample for {}".format(path)))
                if module_cases:
                    cases.extend(module_cases)
                    covered_modules.add((side, module))
                elif (side, module) not in covered_modules:
                    self.skips.append((side, module, "no safe auto GET endpoint"))
        return dedupe_cases(cases)

    def is_safe_get(self, path):
        segments = [segment for segment in path.strip("/").split("/") if segment]
        if path.startswith("/api/front/order/express/"):
            return False
        for segment in segments:
            lowered = segment.lower()
            if lowered in SKIP_SEGMENTS:
                return False
            if lowered.startswith("update") or lowered.startswith("puton") or lowered.startswith("offshell"):
                return False
        if "authorize" in segments:
            return False
        if path.endswith("/push"):
            return False
        return True

    def endpoint_skip_reason(self, path):
        return SKIP_PATH_REASONS.get(path)

    def build_case(self, side, module, method, path):
        replaced = self.replace_path_vars(path)
        if not replaced:
            return None
        token_kind = ""
        if side == "admin" and replaced.startswith("/api/admin/"):
            token_kind = "admin"
        elif side == "front" and replaced.startswith("/api/front/"):
            token_kind = "front"
        params = self.default_params(replaced)
        required = REQUIRED_PARAMS.get(replaced, ())
        if required and any(not params.get(name) for name in required):
            return None
        return {
            "side": side,
            "module": module,
            "method": method,
            "path": replaced,
            "params": params,
            "token": token_kind,
        }

    def replace_path_vars(self, path):
        def value_for(match):
            name = match.group(1)
            value = self.path_value(path, name)
            if value is None or value == "":
                raise KeyError(name)
            return urllib.parse.quote(str(value))

        try:
            return re.sub(r"\{([^}/]+)\}", value_for, path)
        except KeyError:
            return None

    def path_value(self, path, name):
        lower = path.lower()
        if name in {"pid", "pId"}:
            return "0"
        if name in {"type", "status"}:
            return "1"
        if name in {"ids"}:
            return self.context["product_id"]
        if name in {"cid"}:
            return self.context["article_cid"]
        if name in {"timeId"}:
            return self.context["seckill_time_id"]
        if name in {"orderId", "orderNo"}:
            return self.context["order_no"]
        if name in {"preOrderNo", "pinkId", "vCode", "jobId", "ordid"}:
            return None
        if name == "id":
            if "sku" in lower:
                return self.context["product_id"]
            if "seckill" in lower:
                return self.context["seckill_id"]
            if "bargain" in lower:
                return self.context["bargain_id"]
            if "combination" in lower:
                return self.context["combination_id"]
            if "address" in lower:
                return self.context["address_id"]
            if "article" in lower:
                return self.context["article_id"]
            if "category" in lower:
                return self.context["category_id"]
            if "pagediy" in lower:
                return self.context["page_diy_id"]
            if "role" in lower:
                return self.context["role_id"]
            if "attachment" in lower:
                return self.context["attachment_id"]
            if "store/product/rule" in lower:
                return self.context["product_rule_id"]
            if "shipping/templates" in lower:
                return self.context["shipping_template_id"]
            if "system/store" in lower:
                return self.context["store_id"]
            return self.context["product_id"]
        return None

    def default_params(self, path):
        params = {"page": 1, "limit": 5}
        if path == "/api/admin/pagediy/list":
            params["name"] = ""
        if path == "/api/admin/system/config/info":
            params["formId"] = 1
        if path.endswith("/statistics") or "statistics" in path:
            params.setdefault("dateLimit", "today")
        if path == "/api/front/bargain/user":
            params["bargainId"] = self.context["bargain_id"]
        if path == "/api/front/cart/list":
            params["isValid"] = "true"
        if path == "/api/front/cart/count":
            params["type"] = "total"
            params["numType"] = "true"
        if path == "/api/front/combination/more":
            params["comId"] = self.context["combination_id"]
        if path == "/api/front/pay/queryPayResult" and self.context["order_no"]:
            params["orderNo"] = self.context["order_no"]
        if path.startswith("/api/front/reply/list/"):
            params["type"] = 0
        if path == "/api/front/order/list":
            params["type"] = 0
        if path == "/api/front/brokerage_rank":
            params["type"] = "week"
        if path == "/api/front/user/brokerageRankNumber":
            params["type"] = "week"
        if path == "/api/front/coupon/list":
            params["type"] = "usable"
        if path == "/api/front/recharge/bill/record":
            params["type"] = "all"
        if path == "/api/front/wechat/config":
            params["url"] = self.args.front_api.rstrip("/") + "/"
        if path == "/api/front/wechat/program/my/temp/list":
            params["type"] = "afterPay"
        if path == "/api/admin/category/info":
            params["id"] = self.context["category_id"]
        if path == "/api/admin/category/list/tree":
            params["type"] = 1
            params["status"] = 1
        if path == "/api/admin/category/list/ids":
            params["ids"] = self.context["category_id"]
        if path == "/api/admin/export/excel/product":
            params["type"] = 2
        if path == "/api/admin/export/excel/order":
            params["type"] = 0
        if path == "/api/admin/express/all":
            params["type"] = "normal"
        if path == "/api/admin/express/info" and self.context["express_id"]:
            params["id"] = self.context["express_id"]
        if path == "/api/admin/express/template":
            params["com"] = "shunfeng"
        if path == "/api/admin/express/shipping/free/list":
            params["tempId"] = self.context["shipping_template_id"] or 1
        if path == "/api/admin/express/shipping/region/list":
            params["tempId"] = self.context["shipping_template_id"] or 1
        if path == "/api/admin/store/order/info" and self.context["order_no"]:
            params["orderNo"] = self.context["order_no"]
        if path == "/api/admin/store/order/getLogisticsInfo" and self.context["order_no"]:
            params["orderNo"] = self.context["order_no"]
        if path == "/api/admin/store/order/time":
            params["dateLimit"] = "today"
            params["type"] = 1
        if path == "/api/admin/store/product/list":
            params["type"] = 2
        if path == "/api/admin/store/bargain/info":
            params["id"] = self.context["bargain_id"]
        if path == "/api/admin/store/combination/info":
            params["id"] = self.context["combination_id"]
        if path == "/api/admin/store/seckill/info":
            params["id"] = self.context["seckill_id"]
        if path == "/api/admin/store/seckill/manger/info" and self.context["seckill_manger_id"]:
            params["id"] = self.context["seckill_manger_id"]
        if path == "/api/admin/system/admin/info" and self.context["admin_id"]:
            params["id"] = self.context["admin_id"]
        if path == "/api/admin/system/attachment/list":
            params["pid"] = 0
            params["attType"] = "jpg,jpeg,gif,png,bmp,PNG,JPG"
        if path == "/api/admin/system/city/list":
            params["parentId"] = 0
        if path == "/api/admin/system/city/info" and self.context["city_id"]:
            params["id"] = self.context["city_id"]
        if path == "/api/admin/system/notification/detail":
            params["id"] = self.context["notification_id"] or 1
            params["detailType"] = "sms"
        if path == "/api/admin/system/form/temp/info" and self.context["form_temp_id"]:
            params["id"] = self.context["form_temp_id"]
        if path == "/api/admin/system/store/info" and self.context["store_id"]:
            params["id"] = self.context["store_id"]
        if path == "/api/admin/system/store/staff/info" and self.context["staff_id"]:
            params["id"] = self.context["staff_id"]
        if path == "/api/admin/user/infobycondition" and self.context["user_id"]:
            params["userId"] = self.context["user_id"]
            params["type"] = 0
        if path == "/api/admin/user/topdetail" and self.context["user_id"]:
            params["userId"] = self.context["user_id"]
        if path == "/api/admin/user/operate/founds" and self.context["user_id"]:
            params["userId"] = self.context["user_id"]
        if path == "/api/admin/user/group/info" and self.context["user_group_id"]:
            params["id"] = self.context["user_group_id"]
        if path == "/api/admin/user/tag/info" and self.context["user_tag_id"]:
            params["id"] = self.context["user_tag_id"]
        if path == "/api/admin/wechat/config":
            params["url"] = self.args.front_api.rstrip("/") + "/"
        if path == "/api/admin/wechat/keywords/reply/info/keywords":
            params["keywords"] = "smoke"
        if path == "/api/admin/wechat/keywords/reply/info":
            params["id"] = 1
        if path == "/api/admin/store/order/list":
            params.setdefault("type", 2)
        if path == "/api/admin/store/order/status/list" and self.context["order_no"]:
            params["orderNo"] = self.context["order_no"]
        if path.endswith("/info") and "id" not in params:
            if "article" in path:
                params["id"] = self.context["article_id"]
            elif "user" in path and self.context["user_id"]:
                params["id"] = self.context["user_id"]
            elif "system/group/data" in path and self.context["system_group_data_id"]:
                params["id"] = self.context["system_group_data_id"]
            elif "system/group" in path and self.context["system_group_id"]:
                params["id"] = self.context["system_group_id"]
            elif "store/product/rule" in path and self.context["product_rule_id"]:
                params["id"] = self.context["product_rule_id"]
            elif "shipping/templates" in path and self.context["shipping_template_id"]:
                params["id"] = self.context["shipping_template_id"]
        return params

    def run_case(self, case):
        side = case["side"]
        base = self.args.admin_api if side == "admin" else self.args.front_api
        token_kind = case.get("token") or ""
        token = self.admin_token if token_kind == "admin" else self.front_token if token_kind == "front" else ""
        status, content_type, payload = json_call(
            base,
            case["method"],
            case["path"],
            token=token,
            params=case.get("params"),
            body=case.get("json"),
            timeout=self.args.timeout,
        )
        ok, parsed = api_ok(status, payload)
        result = {
            "ok": ok,
            "side": side,
            "module": case["module"],
            "method": case["method"],
            "path": case["path"],
            "url": join_url(base, case["path"], case.get("params")),
            "status": status,
            "content_type": content_type,
            "body": preview(payload),
            "code": parsed.get("code") if isinstance(parsed, dict) else None,
        }
        self.results.append(result)
        prefix = "PASS" if ok else "FAIL"
        print(
            "{} [{}] {} {} -> HTTP {} code {}".format(
                prefix, case["module"], case["method"], result["url"], status, result["code"]
            )
        )
        if not ok:
            print("  {}".format(result["body"]))

    def print_summary(self, cases):
        failures = [item for item in self.results if not item["ok"]]
        covered = {(item["side"], item["module"]) for item in self.results if item["ok"]}
        print("")
        print("Covered modules: {}".format(len(covered)))
        print("Executed API checks: {}".format(len(self.results)))
        print("Skipped endpoints/modules: {}".format(len(self.skips)))
        if self.args.show_skips:
            for side, module, reason in self.skips:
                print("SKIP [{}:{}] {}".format(side, module, reason))
        if failures:
            print("")
            print("{} full API smoke check(s) failed.".format(len(failures)), file=sys.stderr)
            return_code = 1
        else:
            print("")
            print("All full API smoke checks passed.")
            return_code = 0
        raise SystemExit(return_code)


def normalize_path(path):
    return "/" + re.sub(r"/+", "/", path).strip("/")


def dedupe_cases(cases):
    seen = set()
    unique = []
    for case in cases:
        key = (
            case["side"],
            case["method"],
            case["path"],
            tuple(sorted((case.get("params") or {}).items())),
        )
        if key in seen:
            continue
        seen.add(key)
        unique.append(case)
    return unique


def main():
    parser = argparse.ArgumentParser(description="CRMEB full module API smoke tests")
    parser.add_argument("--admin-api", default="http://127.0.0.1:20400")
    parser.add_argument("--front-api", default="http://127.0.0.1:20410")
    parser.add_argument("--admin-account", default="admin")
    parser.add_argument("--admin-password", default="123456")
    parser.add_argument("--front-account", default="18800001001")
    parser.add_argument("--front-password", default="Test123456")
    parser.add_argument("--timeout", type=int, default=DEFAULT_TIMEOUT)
    parser.add_argument("--show-skips", action="store_true")
    args = parser.parse_args()
    FullApiSmoke(args).run()


if __name__ == "__main__":
    main()
