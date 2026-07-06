#!/usr/bin/env python3
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request


BASE_URL = os.environ.get("BASE_URL", "http://127.0.0.1:21410").rstrip("/")
DB_NAME = os.environ.get("DB_NAME", "crmeb")
MYSQL_CONTAINER = os.environ.get("MYSQL_CONTAINER", "crmeb-mysql")
MYSQL_USER = os.environ.get("MYSQL_USER", "root")
MYSQL_PASSWORD = os.environ.get("MYSQL_PASSWORD", "root123456")
FRONT_ACCOUNT = os.environ.get("FRONT_ACCOUNT")
FRONT_PASSWORD = os.environ.get("FRONT_PASSWORD")
REQUIRE_ACTIVE = os.environ.get("REQUIRE_ACTIVE", "1") != "0"

PRODUCT_TYPE_NORMAL = 0
PRODUCT_TYPE_BARGAIN = 2
PRODUCT_TYPE_PINGTUAN = 3

errors = []


def ok(message):
    print("[OK] " + message)


def warn(message):
    print("[WARN] " + message)


def fail(message):
    errors.append(message)
    print("[FAIL] " + message)


def require(condition, message):
    if not condition:
        fail(message)


def request_json(path, method="GET", body=None, token=None):
    url = BASE_URL + path
    data = None
    headers = {"Accept": "application/json"}
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authori-zation"] = token
    request = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            payload = response.read().decode("utf-8")
    except urllib.error.HTTPError as exc:
        payload = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError("{} {} failed: HTTP {} {}".format(method, path, exc.code, payload))
    except urllib.error.URLError as exc:
        raise RuntimeError("{} {} failed: {}".format(method, path, exc.reason))
    try:
        return json.loads(payload)
    except json.JSONDecodeError as exc:
        raise RuntimeError("{} {} returned invalid JSON: {}".format(method, path, exc))


def assert_api_ok(path, method="GET", body=None, token=None):
    try:
        result = request_json(path, method=method, body=body, token=token)
    except Exception as exc:
        fail(str(exc))
        return {}
    if result.get("code") != 200:
        fail("{} {} returned code={} message={}".format(method, path, result.get("code"), result.get("message")))
    else:
        ok("api {} {}".format(method, path))
    return result


def mysql(sql):
    command = [
        "docker",
        "exec",
        MYSQL_CONTAINER,
        "mysql",
        "-u{}".format(MYSQL_USER),
        "--batch",
        "--raw",
        "--skip-column-names",
    ]
    if MYSQL_PASSWORD:
        command.append("-p{}".format(MYSQL_PASSWORD))
    command.extend([DB_NAME, "-e", sql])
    result = subprocess.run(command, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode != 0:
        raise RuntimeError("mysql failed: {}\nSQL: {}".format(result.stderr.strip(), sql))
    rows = []
    for line in result.stdout.splitlines():
        if line.strip():
            rows.append(line.split("\t"))
    return rows


def scalar(sql, default=0):
    rows = mysql(sql)
    if not rows:
        return default
    return int(rows[0][0])


def login():
    result = assert_api_ok(
        "/api/front/login",
        method="POST",
        body={"account": FRONT_ACCOUNT, "password": FRONT_PASSWORD, "spread_spid": 0},
    )
    token = ((result.get("data") or {}).get("token"))
    require(bool(token), "front login did not return a token")
    return token


def active_combinations(now_ms):
    return mysql(
        "select id, product_id from eb_store_combination "
        "where is_del = 0 and is_show = 1 and start_time <= {now} and stop_time >= {now} "
        "order by id".format(now=now_ms)
    )


def active_bargains(now_ms):
    return mysql(
        "select id, product_id from eb_store_bargain "
        "where is_del = 0 and status = 1 and start_time <= {now} and stop_time >= {now} "
        "order by id".format(now=now_ms)
    )


def active_pinks(now_ms):
    return mysql(
        "select p.id from eb_store_pink p "
        "join eb_store_combination c on c.id = p.cid "
        "where p.is_refund = 0 and p.status = 1 and p.k_id = 0 "
        "and c.is_del = 0 and c.is_show = 1 and c.start_time <= {now} and c.stop_time >= {now} "
        "order by p.id limit 10".format(now=now_ms)
    )


def check_activity_sku(activity_name, activity_table, activity_id, product_id, activity_type, require_attr):
    if require_attr:
        activity_attr_count = scalar(
            "select count(*) from eb_store_product_attr where product_id = {} and type = {}".format(
                activity_id, activity_type
            )
        )
        require(activity_attr_count > 0, "{} {} has no activity attrs".format(activity_name, activity_id))

    activity_value_count = scalar(
        "select count(*) from eb_store_product_attr_value where product_id = {} and type = {}".format(
            activity_id, activity_type
        )
    )
    master_value_count = scalar(
        "select count(*) from eb_store_product_attr_value where product_id = {} and type = {}".format(
            product_id, PRODUCT_TYPE_NORMAL
        )
    )
    require(activity_value_count > 0, "{} {} has no activity SKU values".format(activity_name, activity_id))
    require(master_value_count > 0, "{} {} linked product {} has no master SKU values".format(activity_name, activity_id, product_id))

    mismatch_rows = mysql(
        "select av.suk from eb_store_product_attr_value av "
        "left join eb_store_product_attr_value mv "
        "on mv.product_id = {product_id} and mv.type = {normal_type} and mv.suk = av.suk "
        "where av.product_id = {activity_id} and av.type = {activity_type} and mv.id is null".format(
            product_id=product_id,
            normal_type=PRODUCT_TYPE_NORMAL,
            activity_id=activity_id,
            activity_type=activity_type,
        )
    )
    require(
        not mismatch_rows,
        "{} {} has activity SKUs missing in master product: {}".format(
            activity_name, activity_id, ",".join(row[0] for row in mismatch_rows)
        ),
    )

    if not errors:
        ok("db {} {} sku integrity".format(activity_name, activity_id))


def check_combination_api(token, combinations, pinks):
    assert_api_ok("/api/front/combination/index", token=token)
    assert_api_ok("/api/front/combination/header", token=token)
    assert_api_ok("/api/front/combination/list?page=1&limit=10", token=token)
    for row in combinations[:10]:
        activity_id = row[0]
        detail = assert_api_ok("/api/front/combination/detail/{}".format(activity_id), token=token)
        data = detail.get("data") or {}
        require(bool(data.get("productAttr")), "combination detail {} productAttr is empty".format(activity_id))
        require(bool(data.get("productValue")), "combination detail {} productValue is empty".format(activity_id))
        assert_api_ok("/api/front/combination/more?comId={}&page=1&limit=5".format(activity_id), token=token)
    for row in pinks:
        pink_id = row[0]
        result = assert_api_ok("/api/front/combination/pink/{}".format(pink_id), token=token)
        combo = ((result.get("data") or {}).get("storeCombination") or {})
        require(bool(combo.get("productAttr")), "combination pink {} productAttr is empty".format(pink_id))
        require(bool(combo.get("productValue")), "combination pink {} productValue is empty".format(pink_id))


def check_combination_pre_order_master_sku_mapping(token, combinations):
    for row in combinations[:10]:
        activity_id = int(row[0])
        product_id = int(row[1])
        sku_rows = mysql(
            "select av.id, mv.id, av.suk from eb_store_product_attr_value av "
            "join eb_store_product_attr_value mv "
            "on mv.product_id = {product_id} and mv.type = {normal_type} and mv.suk = av.suk and mv.is_del = 0 "
            "where av.product_id = {activity_id} and av.type = {activity_type} and av.is_del = 0 "
            "and av.stock > 0 and av.quota > 0 order by av.id limit 1".format(
                product_id=product_id,
                normal_type=PRODUCT_TYPE_NORMAL,
                activity_id=activity_id,
                activity_type=PRODUCT_TYPE_PINGTUAN,
            )
        )
        require(bool(sku_rows), "combination {} has no purchasable activity/master SKU pair".format(activity_id))
        if not sku_rows:
            continue

        activity_attr_value_id = int(sku_rows[0][0])
        master_attr_value_id = int(sku_rows[0][1])
        result = assert_api_ok(
            "/api/front/order/pre/order",
            method="POST",
            body={
                "preOrderType": "buyNow",
                "orderDetails": [{
                    "attrValueId": master_attr_value_id,
                    "combinationId": activity_id,
                    "productNum": 1,
                    "productId": product_id,
                }],
            },
            token=token,
        )
        pre_order_no = ((result.get("data") or {}).get("preOrderNo"))
        require(bool(pre_order_no), "combination {} master SKU pre-order did not return preOrderNo".format(activity_id))
        if not pre_order_no:
            continue

        loaded = assert_api_ok("/api/front/order/load/pre/{}".format(pre_order_no), token=token)
        order_info = ((loaded.get("data") or {}).get("orderInfoVo") or {})
        detail_list = order_info.get("orderDetailList") or []
        detail = detail_list[0] if detail_list else {}
        require(
            detail.get("attrValueId") == activity_attr_value_id,
            "combination {} master SKU {} did not map to activity SKU {}, got {}".format(
                activity_id,
                master_attr_value_id,
                activity_attr_value_id,
                detail.get("attrValueId"),
            ),
        )


def check_bargain_api(token, bargains):
    assert_api_ok("/api/front/bargain/index", token=token)
    assert_api_ok("/api/front/bargain/header", token=token)
    assert_api_ok("/api/front/bargain/list?page=1&limit=10", token=token)
    for row in bargains[:10]:
        activity_id = row[0]
        result = assert_api_ok("/api/front/bargain/detail/{}".format(activity_id), token=token)
        data = result.get("data") or {}
        require(data.get("attrValueId") is not None, "bargain detail {} attrValueId is empty".format(activity_id))
        require(bool(data.get("sku")), "bargain detail {} sku is empty".format(activity_id))


def main():
    print("Activity smoke BASE_URL={} DB_NAME={}".format(BASE_URL, DB_NAME))
    if not FRONT_ACCOUNT or not FRONT_PASSWORD:
        raise SystemExit("FRONT_ACCOUNT and FRONT_PASSWORD must be set for template-store activity smoke tests")
    token = login()
    now_ms = int(time.time() * 1000)
    combinations = active_combinations(now_ms)
    bargains = active_bargains(now_ms)
    pinks = active_pinks(now_ms)

    if REQUIRE_ACTIVE:
        require(bool(combinations), "no active group-buy activities found")
        require(bool(bargains), "no active bargain activities found")
    elif not combinations:
        warn("no active group-buy activities found")
    elif not bargains:
        warn("no active bargain activities found")

    for row in combinations:
        check_activity_sku("combination", "eb_store_combination", int(row[0]), int(row[1]), PRODUCT_TYPE_PINGTUAN, True)
    for row in bargains:
        check_activity_sku("bargain", "eb_store_bargain", int(row[0]), int(row[1]), PRODUCT_TYPE_BARGAIN, True)

    check_combination_api(token, combinations, pinks)
    check_combination_pre_order_master_sku_mapping(token, combinations)
    check_bargain_api(token, bargains)

    if errors:
        print("\nActivity smoke failed:")
        for message in errors:
            print("- " + message)
        return 1
    print("\nActivity smoke passed: {} combinations, {} bargains, {} active pinks checked".format(
        len(combinations), len(bargains), len(pinks)
    ))
    return 0


if __name__ == "__main__":
    sys.exit(main())
