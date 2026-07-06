#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MYSQL_IMAGE="${MYSQL_IMAGE:-mysql:5.7}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root123456}"
DB_NAME="${DB_NAME:-crmeb}"
CONTAINER_NAME="${CONTAINER_NAME:-crmeb-template-sql-verify-$$}"

cleanup() {
  docker rm -f "${CONTAINER_NAME}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker run -d \
  --name "${CONTAINER_NAME}" \
  -e MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD}" \
  -e MYSQL_DATABASE="${DB_NAME}" \
  -e TZ=Asia/Shanghai \
  -v "${ROOT_DIR}/crmeb/sql/Crmeb_v1.4.sql:/docker-entrypoint-initdb.d/01-crmeb.sql:ro" \
  -v "${ROOT_DIR}/docker/mysql/02-quartz-case.sql:/docker-entrypoint-initdb.d/02-quartz-case.sql:ro" \
  -v "${ROOT_DIR}/crmeb/sql/manual_qr_payment_20260704.sql:/docker-entrypoint-initdb.d/03-manual-qr-payment.sql:ro" \
  -v "${ROOT_DIR}/crmeb/sql/activity_sku_repair_20260705.sql:/docker-entrypoint-initdb.d/04-activity-sku-repair.sql:ro" \
  -v "${ROOT_DIR}/crmeb/sql/template_store_cleanup_20260706.sql:/docker-entrypoint-initdb.d/05-template-store-cleanup.sql:ro" \
  "${MYSQL_IMAGE}" \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_general_ci \
  --max_allowed_packet=256M >/dev/null

mysql_exec() {
  docker exec "${CONTAINER_NAME}" \
    env MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" \
    mysql \
    -uroot \
    --default-character-set=utf8mb4 \
    --batch \
    --raw \
    --skip-column-names \
    "${DB_NAME}" \
    -e "$1"
}

for _ in $(seq 1 120); do
  ready_count="$(mysql_exec "select count(*) from eb_system_config where name = 'site_name' and value = '商城模板';" 2>/dev/null || echo "0")"
  if [[ "${ready_count}" =~ ^[1-9][0-9]*$ ]]; then
    break
  fi
  sleep 2
done

site_name_ready="$(mysql_exec "select count(*) from eb_system_config where name = 'site_name' and value = '商城模板';" 2>/dev/null || echo "0")"
if ! [[ "${site_name_ready}" =~ ^[1-9][0-9]*$ ]]; then
  echo "template SQL did not finish successfully" >&2
  docker logs "${CONTAINER_NAME}" >&2 || true
  exit 1
fi

assert_scalar() {
  local sql="$1"
  local expected="$2"
  local actual
  actual="$(mysql_exec "${sql}")"
  if [[ "${actual}" != "${expected}" ]]; then
    echo "assertion failed: expected '${expected}', got '${actual}'" >&2
    echo "sql: ${sql}" >&2
    exit 1
  fi
}

zero_tables=(
  eb_store_product
  eb_store_product_attr
  eb_store_product_attr_value
  eb_store_product_description
  eb_store_order
  eb_store_order_info
  eb_store_cart
  eb_store_coupon
  eb_store_combination
  eb_store_bargain
  eb_store_seckill
  eb_user
  eb_user_address
  eb_article
  eb_system_attachment
)

for table in "${zero_tables[@]}"; do
  assert_scalar "select count(*) from ${table};" "0"
done

assert_scalar "select group_concat(name order by sort separator ',') from eb_category where type = 1;" "模板分类一,模板分类二,模板分类三"
assert_scalar "select count(*) from eb_shipping_templates where id = 10001 and name = '默认包邮模板' and appoint = 0;" "1"
assert_scalar "select title from eb_page_diy where id = 10001;" "商城模板"
assert_scalar "select real_name from eb_system_admin where account = 'admin';" "模板管理员"
assert_scalar "select count(*) from eb_system_admin;" "1"
assert_scalar "select count(*) from eb_system_config where name = 'pay_mode' and value = 'offline_qr';" "1"
assert_scalar "select count(*) from eb_system_config where name = 'offline_pay_status' and value = '''1''';" "1"
assert_scalar "select count(*) from eb_system_config where name = 'pay_weixin_open' and value = '''0''';" "1"

echo "template SQL verification passed"
