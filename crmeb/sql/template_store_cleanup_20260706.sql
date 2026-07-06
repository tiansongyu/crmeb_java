-- Template store cleanup.
-- Run after the base schema/data and feature migrations. It removes demo
-- business data while keeping operational platform settings and admin access.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `eb_store_coupon_user`;
DELETE FROM `eb_store_coupon`;
DELETE FROM `eb_store_order_status`;
DELETE FROM `eb_store_order_info`;
DELETE FROM `eb_store_order`;
DELETE FROM `eb_store_cart`;
DELETE FROM `eb_store_pink`;
DELETE FROM `eb_store_bargain_user_help`;
DELETE FROM `eb_store_bargain_user`;
DELETE FROM `eb_store_bargain`;
DELETE FROM `eb_store_combination`;
DELETE FROM `eb_store_seckill`;
DELETE FROM `eb_store_seckill_manger`;
DELETE FROM `eb_store_product_coupon`;
DELETE FROM `eb_store_product_cate`;
DELETE FROM `eb_store_product_description`;
DELETE FROM `eb_store_product_log`;
DELETE FROM `eb_store_product_relation`;
DELETE FROM `eb_store_product_reply`;
DELETE FROM `eb_store_product_attr_result`;
DELETE FROM `eb_store_product_attr_value`;
DELETE FROM `eb_store_product_attr`;
DELETE FROM `eb_store_product_rule`;
DELETE FROM `eb_store_product`;

DELETE FROM `eb_article`;
DELETE FROM `eb_activity_style`;
DELETE FROM `eb_page_diy`;
DELETE FROM `eb_page_category`;
DELETE FROM `eb_page_link`;
DELETE FROM `eb_system_group_data`;
DELETE FROM `eb_system_attachment`;
DELETE FROM `eb_shipping_templates_region`;
DELETE FROM `eb_shipping_templates_free`;
DELETE FROM `eb_shipping_templates`;
DELETE FROM `eb_system_store_staff`;
DELETE FROM `eb_system_store`;

DELETE FROM `eb_wechat_reply`;
DELETE FROM `eb_wechat_callback`;
DELETE FROM `eb_wechat_exceptions`;
DELETE FROM `eb_wechat_pay_info`;
DELETE FROM `eb_wechat_program_my_temp`;

DELETE FROM `eb_user_visit_record`;
DELETE FROM `eb_user_token`;
DELETE FROM `eb_user_sign`;
DELETE FROM `eb_user_recharge`;
DELETE FROM `eb_user_integral_record`;
DELETE FROM `eb_user_extract`;
DELETE FROM `eb_user_experience_record`;
DELETE FROM `eb_user_brokerage_record`;
DELETE FROM `eb_user_bill`;
DELETE FROM `eb_user_address`;
DELETE FROM `eb_user_level`;
DELETE FROM `eb_user`;

DELETE FROM `eb_schedule_job_log`;
DELETE FROM `eb_user_group`;
DELETE FROM `eb_user_tag`;
DELETE FROM `eb_category` WHERE `type` = 1;
DELETE FROM `eb_system_admin` WHERE `account` <> 'admin';

INSERT INTO `eb_user_group` (`id`, `group_name`) VALUES
  (1, '模板用户分组')
ON DUPLICATE KEY UPDATE
  `group_name` = VALUES(`group_name`);

INSERT INTO `eb_user_tag` (`id`, `name`) VALUES
  (1, '模板标签')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`);

INSERT INTO `eb_category` (`id`, `pid`, `path`, `name`, `type`, `url`, `extra`, `status`, `sort`, `create_time`, `update_time`) VALUES
  (10001, 0, '/0/', '模板分类一', 1, '', '', 1, 1, NOW(), NOW()),
  (10002, 0, '/0/', '模板分类二', 1, '', '', 1, 2, NOW(), NOW()),
  (10003, 0, '/0/', '模板分类三', 1, '', '', 1, 3, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `pid` = VALUES(`pid`),
  `path` = VALUES(`path`),
  `name` = VALUES(`name`),
  `type` = VALUES(`type`),
  `url` = VALUES(`url`),
  `extra` = VALUES(`extra`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = NOW();

INSERT INTO `eb_page_diy` (
  `id`, `version`, `name`, `title`, `cover_image`, `template_name`, `value`,
  `add_time`, `update_time`, `status`, `type`, `is_show`, `is_bg_color`,
  `is_bg_pic`, `is_diy`, `color_picker`, `bg_pic`, `bg_tab_val`, `is_del`,
  `return_address`, `title_bg_color`, `title_color`, `service_status`,
  `mer_id`, `is_default`, `text_position`
) VALUES (
  10001, '', '模板首页', '商城模板', '', '', '{}',
  NOW(), NOW(), 0, 0, 1, 1,
  0, 0, '#f5f5f5', '', 0, 0,
  '', '#fff', '#000000', 1,
  0, 1, 0
) ON DUPLICATE KEY UPDATE
  `version` = VALUES(`version`),
  `name` = VALUES(`name`),
  `title` = VALUES(`title`),
  `cover_image` = VALUES(`cover_image`),
  `template_name` = VALUES(`template_name`),
  `value` = VALUES(`value`),
  `update_time` = NOW(),
  `status` = VALUES(`status`),
  `type` = VALUES(`type`),
  `is_show` = VALUES(`is_show`),
  `is_bg_color` = VALUES(`is_bg_color`),
  `is_bg_pic` = VALUES(`is_bg_pic`),
  `is_diy` = VALUES(`is_diy`),
  `color_picker` = VALUES(`color_picker`),
  `bg_pic` = VALUES(`bg_pic`),
  `bg_tab_val` = VALUES(`bg_tab_val`),
  `is_del` = VALUES(`is_del`),
  `return_address` = VALUES(`return_address`),
  `title_bg_color` = VALUES(`title_bg_color`),
  `title_color` = VALUES(`title_color`),
  `service_status` = VALUES(`service_status`),
  `mer_id` = VALUES(`mer_id`),
  `is_default` = VALUES(`is_default`),
  `text_position` = VALUES(`text_position`);

INSERT INTO `eb_shipping_templates` (`id`, `name`, `type`, `appoint`, `sort`, `create_time`, `update_time`) VALUES
  (10001, '默认包邮模板', 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `type` = VALUES(`type`),
  `appoint` = VALUES(`appoint`),
  `sort` = VALUES(`sort`),
  `update_time` = NOW();

UPDATE `eb_system_admin`
SET `real_name` = '模板管理员',
    `last_ip` = NULL,
    `login_count` = 0,
    `phone` = '',
    `update_time` = NOW()
WHERE `account` = 'admin';

UPDATE `eb_system_config`
SET `value` = '商城模板',
    `update_time` = NOW()
WHERE `name` IN (
  'site_name',
  'seo_title',
  'wechat_share_title',
  'routine_name'
);

UPDATE `eb_system_config`
SET `value` = '商城模板占位数据，请在后台添加商品、装修首页并配置收款码。',
    `update_time` = NOW()
WHERE `name` = 'wechat_share_synopsis';

UPDATE `eb_system_config`
SET `value` = '',
    `update_time` = NOW()
WHERE `name` IN (
  'copyright_company_name',
  'copyright_company_image',
  'copyright_icp_number',
  'copyright_icp_number_url',
  'copyright_internet_record',
  'copyright_internet_record_url',
  'copyright_internet_culture',
  'copyright_internet_culture_url',
  'copyright_network_security',
  'copyright_network_security_url',
  'site_url',
  'api_url',
  'front_api_url',
  'crmeb_tongji_js',
  'wechat_share_img',
  'wechat_qrcode',
  'h5_avatar',
  'site_logo_lefttop',
  'site_logo_square',
  'site_logo_login',
  'admin_login_bg_pic',
  'mobile_top_logo',
  'mobile_login_logo',
  'consumer_hotline',
  'app_update_url',
  'ios_address',
  'android_address',
  'localUploadUrl',
  'offline_pay_qrcode',
  'offline_pay_name'
);

UPDATE `eb_system_config`
SET `value` = '0',
    `update_time` = NOW()
WHERE `name` = 'bottom_navigation_is_custom';

UPDATE `eb_system_config`
SET `value` = 'offline_qr',
    `update_time` = NOW()
WHERE `name` = 'pay_mode';

UPDATE `eb_system_config`
SET `value` = '''1''',
    `update_time` = NOW()
WHERE `name` = 'offline_pay_status';

UPDATE `eb_system_config`
SET `value` = '''0''',
    `update_time` = NOW()
WHERE `name` IN ('pay_weixin_open', 'yue_pay_status', 'ali_pay_status');

UPDATE `eb_system_config`
SET `value` = '请扫码完成转账，转账金额需与订单金额一致，支付后上传付款截图等待后台确认。',
    `update_time` = NOW()
WHERE `name` = 'offline_pay_tips';

INSERT INTO `eb_system_config` (`name`, `title`, `form_id`, `value`, `status`, `create_time`, `update_time`)
SELECT 'site_name', '网站名称', 0, '商城模板', 0, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `eb_system_config` WHERE `name` = 'site_name');

INSERT INTO `eb_system_config` (`name`, `title`, `form_id`, `value`, `status`, `create_time`, `update_time`)
SELECT 'pay_mode', '支付模式', 103, 'offline_qr', 0, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `eb_system_config` WHERE `name` = 'pay_mode');

INSERT INTO `eb_system_config` (`name`, `title`, `form_id`, `value`, `status`, `create_time`, `update_time`)
SELECT 'offline_pay_status', '线下扫码转账支付开关', 79, '''1''', 0, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `eb_system_config` WHERE `name` = 'offline_pay_status');

INSERT INTO `eb_system_config` (`name`, `title`, `form_id`, `value`, `status`, `create_time`, `update_time`)
SELECT 'offline_pay_qrcode', '线下扫码转账收款码', 79, '', 0, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `eb_system_config` WHERE `name` = 'offline_pay_qrcode');

SET FOREIGN_KEY_CHECKS = 1;
