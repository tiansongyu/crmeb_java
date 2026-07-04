-- Manual QR payment migration.
-- Adds offline proof/audit fields and expands the existing offline payment setting.

SET NAMES utf8mb4;
SET @database_name = DATABASE();

SET @sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @database_name AND TABLE_NAME = 'eb_store_order' AND COLUMN_NAME = 'offline_pay_status') = 0,
    'ALTER TABLE `eb_store_order` ADD COLUMN `offline_pay_status` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''线下付款状态：0未提交，1待审核，2已通过，3已驳回'' AFTER `pay_type`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @database_name AND TABLE_NAME = 'eb_store_order' AND COLUMN_NAME = 'offline_pay_voucher') = 0,
    'ALTER TABLE `eb_store_order` ADD COLUMN `offline_pay_voucher` varchar(5000) NULL DEFAULT NULL COMMENT ''线下付款凭证'' AFTER `offline_pay_status`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @database_name AND TABLE_NAME = 'eb_store_order' AND COLUMN_NAME = 'offline_pay_trade_no') = 0,
    'ALTER TABLE `eb_store_order` ADD COLUMN `offline_pay_trade_no` varchar(128) NULL DEFAULT NULL COMMENT ''线下付款交易号'' AFTER `offline_pay_voucher`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @database_name AND TABLE_NAME = 'eb_store_order' AND COLUMN_NAME = 'offline_pay_remark') = 0,
    'ALTER TABLE `eb_store_order` ADD COLUMN `offline_pay_remark` varchar(512) NULL DEFAULT NULL COMMENT ''线下付款备注'' AFTER `offline_pay_trade_no`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @database_name AND TABLE_NAME = 'eb_store_order' AND COLUMN_NAME = 'offline_pay_refuse_reason') = 0,
    'ALTER TABLE `eb_store_order` ADD COLUMN `offline_pay_refuse_reason` varchar(512) NULL DEFAULT NULL COMMENT ''线下付款驳回原因'' AFTER `offline_pay_remark`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @database_name AND TABLE_NAME = 'eb_store_order' AND COLUMN_NAME = 'offline_pay_submit_time') = 0,
    'ALTER TABLE `eb_store_order` ADD COLUMN `offline_pay_submit_time` timestamp NULL DEFAULT NULL COMMENT ''线下付款提交时间'' AFTER `offline_pay_refuse_reason`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @database_name AND TABLE_NAME = 'eb_store_order' AND COLUMN_NAME = 'offline_pay_audit_time') = 0,
    'ALTER TABLE `eb_store_order` ADD COLUMN `offline_pay_audit_time` timestamp NULL DEFAULT NULL COMMENT ''线下付款审核时间'' AFTER `offline_pay_submit_time`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @database_name AND TABLE_NAME = 'eb_store_order' AND COLUMN_NAME = 'offline_pay_audit_admin_id') = 0,
    'ALTER TABLE `eb_store_order` ADD COLUMN `offline_pay_audit_admin_id` int(11) UNSIGNED NULL DEFAULT NULL COMMENT ''线下付款审核管理员ID'' AFTER `offline_pay_audit_time`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @database_name AND TABLE_NAME = 'eb_store_order' AND INDEX_NAME = 'idx_offline_pay_status') = 0,
    'ALTER TABLE `eb_store_order` ADD INDEX `idx_offline_pay_status`(`offline_pay_status`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @database_name AND TABLE_NAME = 'eb_store_order' AND INDEX_NAME = 'idx_pay_type_offline_status') = 0,
    'ALTER TABLE `eb_store_order` ADD INDEX `idx_pay_type_offline_status`(`pay_type`, `offline_pay_status`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `eb_system_config`
SET `title` = '线下扫码转账支付开关',
    `form_id` = 79,
    `value` = '''1''',
    `update_time` = NOW()
WHERE `name` = 'offline_pay_status';

INSERT INTO `eb_system_config` (`name`, `title`, `form_id`, `value`, `status`, `create_time`, `update_time`)
SELECT 'offline_pay_status', '线下扫码转账支付开关', 79, '''1''', 0, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `eb_system_config` WHERE `name` = 'offline_pay_status');

UPDATE `eb_system_config`
SET `title` = '线下扫码转账收款码',
    `form_id` = 79,
    `update_time` = NOW()
WHERE `name` = 'offline_pay_qrcode';

INSERT INTO `eb_system_config` (`name`, `title`, `form_id`, `value`, `status`, `create_time`, `update_time`)
SELECT 'offline_pay_qrcode', '线下扫码转账收款码', 79, '', 0, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `eb_system_config` WHERE `name` = 'offline_pay_qrcode');

UPDATE `eb_system_config`
SET `title` = '线下扫码转账收款名称',
    `form_id` = 79,
    `update_time` = NOW()
WHERE `name` = 'offline_pay_name';

INSERT INTO `eb_system_config` (`name`, `title`, `form_id`, `value`, `status`, `create_time`, `update_time`)
SELECT 'offline_pay_name', '线下扫码转账收款名称', 79, '', 0, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `eb_system_config` WHERE `name` = 'offline_pay_name');

UPDATE `eb_system_config`
SET `title` = '线下扫码转账付款提示',
    `form_id` = 79,
    `update_time` = NOW()
WHERE `name` = 'offline_pay_tips';

INSERT INTO `eb_system_config` (`name`, `title`, `form_id`, `value`, `status`, `create_time`, `update_time`)
SELECT 'offline_pay_tips', '线下扫码转账付款提示', 79, '请扫码完成转账，转账金额需与订单金额一致，支付后上传付款截图等待后台确认。', 0, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `eb_system_config` WHERE `name` = 'offline_pay_tips');

UPDATE `eb_system_form_temp`
SET `name` = '线下支付',
    `info` = '支付设置-线下支付',
    `content` = '{"formRef":"elForm","formModel":"formData","size":"medium","labelPosition":"right","labelWidth":150,"formRules":"rules","gutter":15,"disabled":false,"span":24,"formBtns":true,"fields":[{"__config__":{"label":"线下支付状态：","labelWidth":null,"showLabel":true,"tag":"el-radio-group","tagIcon":"radio","changeTag":true,"layout":"colFormItem","span":24,"optionType":"default","regList":[],"required":true,"border":false,"document":"https://element.eleme.cn/#/zh-CN/component/radio","formId":101,"renderKey":1590041613118,"defaultValue":"''1''","tips":false},"__slot__":{"options":[{"label":"开启","value":"''1''"},{"label":"关闭","value":"''0''"}]},"style":{},"size":"medium","disabled":false,"__vModel__":"offline_pay_status"},{"__config__":{"label":"收款码：","tag":"self-upload","tagIcon":"selfUpload","layout":"colFormItem","defaultValue":null,"showLabel":true,"labelWidth":null,"required":true,"span":24,"showTip":false,"buttonText":"点击上传","regList":[],"changeTag":true,"fileSize":2,"sizeUnit":"MB","document":"https://element.eleme.cn/#/zh-CN/component/upload","formId":102,"renderKey":1780000000102,"tips":false},"__slot__":{"list-type":true},"action":"https://jsonplaceholder.typicode.com/posts/","disabled":true,"accept":"","name":"file","auto-upload":true,"list-type":"picture-card","multiple":false,"__vModel__":"offline_pay_qrcode"},{"__config__":{"label":"收款名称：","labelWidth":null,"showLabel":true,"changeTag":true,"tag":"el-input","tagIcon":"input","required":false,"layout":"colFormItem","span":24,"document":"https://element.eleme.cn/#/zh-CN/component/input","regList":[],"formId":103,"renderKey":1780000000103,"tips":false},"__slot__":{"prepend":"","append":""},"placeholder":"请输入收款账户名称","style":{"width":"50%"},"clearable":true,"prefix-icon":"","suffix-icon":"","maxlength":null,"show-word-limit":false,"readonly":false,"disabled":false,"__vModel__":"offline_pay_name"},{"__config__":{"label":"付款提示：","labelWidth":null,"showLabel":true,"tag":"el-input","tagIcon":"textarea","required":false,"layout":"colFormItem","span":24,"regList":[],"changeTag":true,"document":"https://element.eleme.cn/#/zh-CN/component/input","formId":104,"renderKey":1780000000104,"tips":false},"type":"textarea","placeholder":"请输入付款提示","autosize":{"minRows":4,"maxRows":4},"style":{"width":"50%"},"maxlength":null,"show-word-limit":false,"readonly":false,"disabled":false,"__vModel__":"offline_pay_tips"}]}',
    `update_time` = NOW()
WHERE `id` = 79;

UPDATE `eb_category`
SET `pid` = 103,
    `path` = '/0/103/',
    `name` = '线下支付',
    `type` = 6,
    `url` = '线下支付',
    `status` = 1,
    `sort` = 1,
    `update_time` = NOW()
WHERE `extra` = '79'
  AND `type` = 6;

INSERT INTO `eb_category` (`pid`, `path`, `name`, `type`, `url`, `extra`, `status`, `sort`, `create_time`, `update_time`)
SELECT 103, '/0/103/', '线下支付', 6, '线下支付', '79', 1, 1, NOW(), NOW()
FROM DUAL
WHERE EXISTS (SELECT 1 FROM `eb_category` WHERE `id` = 103)
  AND NOT EXISTS (SELECT 1 FROM `eb_category` WHERE `extra` = '79' AND `type` = 6);
