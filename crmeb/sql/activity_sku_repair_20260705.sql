-- Repair activity SKU rows for legacy group-buy and bargain activities.
-- This script only fills missing activity attrs/SKUs. Existing activity SKU rows are not overwritten.

START TRANSACTION;

INSERT INTO eb_store_product_attr (product_id, attr_name, attr_values, type, is_del)
SELECT b.id, pa.attr_name, pa.attr_values, 2, 0
FROM eb_store_bargain b
JOIN eb_store_product_attr pa ON pa.product_id = b.product_id AND pa.type = 0
WHERE b.is_del = 0
  AND NOT EXISTS (
      SELECT 1 FROM eb_store_product_attr ea WHERE ea.product_id = b.id AND ea.type = 2
  );

INSERT INTO eb_store_product_attr (product_id, attr_name, attr_values, type, is_del)
SELECT c.id, pa.attr_name, pa.attr_values, 3, 0
FROM eb_store_combination c
JOIN eb_store_product_attr pa ON pa.product_id = c.product_id AND pa.type = 0
WHERE c.is_del = 0
  AND NOT EXISTS (
      SELECT 1 FROM eb_store_product_attr ea WHERE ea.product_id = c.id AND ea.type = 3
  );

INSERT INTO eb_store_product_attr_value (
    product_id, suk, stock, sales, price, image, `unique`, cost, bar_code, ot_price,
    weight, volume, brokerage, brokerage_two, type, quota, quota_show, attr_value, is_del, version
)
SELECT
    b.id,
    mv.suk,
    LEAST(b.stock, mv.stock),
    0,
    b.price,
    mv.image,
    SUBSTRING(MD5(CONCAT('bargain:', b.id, ':', mv.id)), 1, 8),
    b.cost,
    mv.bar_code,
    mv.ot_price,
    b.weight,
    b.volume,
    mv.brokerage,
    mv.brokerage_two,
    2,
    b.quota,
    b.quota_show,
    mv.attr_value,
    0,
    0
FROM eb_store_bargain b
JOIN eb_store_product_attr_value mv ON mv.product_id = b.product_id AND mv.type = 0
WHERE b.is_del = 0
  AND NOT EXISTS (
      SELECT 1 FROM eb_store_product_attr_value av WHERE av.product_id = b.id AND av.type = 2
  );

INSERT INTO eb_store_product_attr_value (
    product_id, suk, stock, sales, price, image, `unique`, cost, bar_code, ot_price,
    weight, volume, brokerage, brokerage_two, type, quota, quota_show, attr_value, is_del, version
)
SELECT
    c.id,
    mv.suk,
    LEAST(c.stock, mv.stock),
    0,
    c.price,
    mv.image,
    SUBSTRING(MD5(CONCAT('combination:', c.id, ':', mv.id)), 1, 8),
    c.cost,
    mv.bar_code,
    c.ot_price,
    c.weight,
    c.volume,
    mv.brokerage,
    mv.brokerage_two,
    3,
    c.quota,
    c.quota_show,
    mv.attr_value,
    0,
    0
FROM eb_store_combination c
JOIN eb_store_product_attr_value mv ON mv.product_id = c.product_id AND mv.type = 0
WHERE c.is_del = 0
  AND NOT EXISTS (
      SELECT 1 FROM eb_store_product_attr_value av WHERE av.product_id = c.id AND av.type = 3
  );

COMMIT;
