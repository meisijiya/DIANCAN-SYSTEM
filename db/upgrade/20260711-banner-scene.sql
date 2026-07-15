ALTER TABLE `home_banner`
  ADD COLUMN `scene` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'HOME' COMMENT '投放位置（HOME首页轮播 MENU_HERO点餐页头图 MENU_BANNER点餐页轮播 PROFILE_HERO我的页头图）' AFTER `target_path`,
  ADD INDEX `idx_home_banner_scene_status_sort` (`scene`, `status`, `sort`);

UPDATE `home_banner`
SET `scene` = 'HOME'
WHERE `scene` IS NULL OR `scene` = '';
