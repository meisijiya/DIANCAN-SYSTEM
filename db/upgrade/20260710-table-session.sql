ALTER TABLE `dining_table`
  ADD COLUMN `current_session_code` varchar(64) NULL DEFAULT NULL COMMENT '当前桌次编码' AFTER `area_name`;

ALTER TABLE `dining_table`
  ADD INDEX `idx_current_session_code` (`current_session_code`);

ALTER TABLE `order`
  ADD COLUMN `table_session_code` varchar(64) NULL DEFAULT NULL COMMENT '桌次编码（冗余）' AFTER `table_code`;

ALTER TABLE `order`
  ADD INDEX `idx_table_session_status` (`table_id`, `table_session_code`, `status`);

UPDATE `dining_table`
SET `current_session_code` = NULL
WHERE `status` = 0;

UPDATE `dining_table`
SET `current_session_code` = CONCAT('TS', REPLACE(UUID(), '-', ''))
WHERE `status` <> 0
  AND (`current_session_code` IS NULL OR `current_session_code` = '');

UPDATE `order` o
JOIN `dining_table` t ON t.`id` = o.`table_id`
SET o.`table_session_code` = t.`current_session_code`
WHERE o.`status` IN (0, 1)
  AND (o.`table_session_code` IS NULL OR o.`table_session_code` = '')
  AND t.`status` <> 0;
