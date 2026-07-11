/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80407 (8.4.7)
 Source Host           : 127.0.0.1:3306
 Source Schema         : digital_ordering_system

 Target Server Type    : MySQL
 Target Server Version : 80407 (8.4.7)
 File Encoding         : 65001

 Date: 07/07/2026 23:04:57
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for audit_log_export_task
-- ----------------------------
DROP TABLE IF EXISTS `audit_log_export_task`;
CREATE TABLE `audit_log_export_task`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `task_status` tinyint NOT NULL DEFAULT 0 COMMENT '任务状态（0待处理 1处理中 2成功 3失败）',
  `start_date` date NULL DEFAULT NULL COMMENT '筛选开始日期',
  `end_date` date NULL DEFAULT NULL COMMENT '筛选结束日期',
  `operator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作人筛选',
  `operation_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作类型筛选',
  `total_count` int NOT NULL DEFAULT 0 COMMENT '总记录数',
  `exported_count` int NOT NULL DEFAULT 0 COMMENT '已导出记录数',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '导出文件名',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '导出文件路径',
  `last_error` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后错误信息',
  `started_time` datetime NULL DEFAULT NULL COMMENT '开始处理时间',
  `finished_time` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `create_by` bigint NULL DEFAULT NULL,
  `update_by` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_audit_log_export_task_creator_status`(`create_by` ASC, `task_status` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '审计日志导出任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of audit_log_export_task
-- ----------------------------

-- ----------------------------
-- Table structure for coupon_grant_task
-- ----------------------------
DROP TABLE IF EXISTS `coupon_grant_task`;
CREATE TABLE `coupon_grant_task`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `template_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `grant_mode` tinyint NOT NULL COMMENT '发放方式（1指定用户 2全部用户）',
  `task_status` tinyint NOT NULL DEFAULT 0 COMMENT '任务状态（0待处理 1已完成 2部分完成）',
  `target_count` int NOT NULL DEFAULT 0 COMMENT '目标人数',
  `success_count` int NOT NULL DEFAULT 0 COMMENT '成功人数',
  `fail_count` int NOT NULL DEFAULT 0 COMMENT '失败人数',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `started_time` datetime NULL DEFAULT NULL COMMENT '开始处理时间',
  `last_error` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后错误信息',
  `total_batch_count` int NOT NULL DEFAULT 0 COMMENT '总批次数',
  `finished_batch_count` int NOT NULL DEFAULT 0 COMMENT '已完成批次数',
  `finished_time` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `create_by` bigint NULL DEFAULT NULL,
  `update_by` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '发券任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of coupon_grant_task
-- ----------------------------

-- ----------------------------
-- Table structure for coupon_grant_task_user
-- ----------------------------
DROP TABLE IF EXISTS `coupon_grant_task_user`;
CREATE TABLE `coupon_grant_task_user`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '发券任务ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户名快照',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号快照',
  `grant_status` tinyint NOT NULL DEFAULT 0 COMMENT '发放状态（0待处理 1成功 2失败 3跳过）',
  `fail_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '失败原因',
  `finished_time` datetime NULL DEFAULT NULL COMMENT '处理完成时间',
  `create_by` bigint NULL DEFAULT NULL,
  `update_by` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_coupon_task_user`(`task_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_coupon_task_user_status`(`task_id` ASC, `grant_status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '发券任务用户快照表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of coupon_grant_task_user
-- ----------------------------

-- ----------------------------
-- Table structure for coupon_template
-- ----------------------------
DROP TABLE IF EXISTS `coupon_template`;
CREATE TABLE `coupon_template`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `type` tinyint NOT NULL COMMENT '优惠券类型（1满减 2折扣）',
  `threshold_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛金额',
  `discount_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  `discount_rate` decimal(4, 2) NULL DEFAULT NULL COMMENT '折扣比例',
  `total_quantity` int NOT NULL DEFAULT 0 COMMENT '发放总量（0不限量）',
  `issued_quantity` int NOT NULL DEFAULT 0 COMMENT '已发放数量',
  `per_user_limit` int NOT NULL DEFAULT 0 COMMENT '每人限领张数（0不限）',
  `validity_type` tinyint NOT NULL COMMENT '有效期类型（1固定时间 2领券后N天）',
  `valid_from` datetime NULL DEFAULT NULL COMMENT '固定生效时间',
  `valid_to` datetime NULL DEFAULT NULL COMMENT '固定失效时间',
  `valid_days` int NULL DEFAULT NULL COMMENT '领券后有效天数',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0停用 1启用）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '说明',
  `available_weekdays` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '可用星期，1-7 表示周一到周日，逗号分隔，NULL 表示每天可用',
  `create_by` bigint NULL DEFAULT NULL,
  `update_by` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_coupon_template_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '优惠券模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of coupon_template
-- ----------------------------
INSERT INTO `coupon_template` VALUES (30001, '新客到店券', 1, 68.00, 8.00, NULL, 0, 0, 1, 1, '2026-06-26 23:33:32', '2026-07-26 23:33:32', NULL, 1, '满68减8，适合首次到店体验', NULL, 1, 1, '2026-06-26 23:33:32', '2026-06-27 14:04:34', 0);
INSERT INTO `coupon_template` VALUES (30002, '人气热菜券', 1, 100.00, 12.00, NULL, 0, 0, 1, 1, '2026-06-26 23:33:32', '2026-07-26 23:33:32', NULL, 1, '满100减12，适合多人堂食点单', NULL, 1, 1, '2026-06-26 23:33:32', '2026-06-26 23:34:01', 0);
INSERT INTO `coupon_template` VALUES (30003, '周末聚餐券', 1, 200.00, 30.00, NULL, 0, 0, 1, 1, '2026-06-26 23:33:32', '2026-08-10 23:33:32', NULL, 1, '满200减30，适合多人聚餐使用', '6,7', 1, 1, '2026-06-26 23:33:32', '2026-06-27 14:40:59', 0);
INSERT INTO `coupon_template` VALUES (30004, '饮品甜点折扣券', 2, 88.00, 0.00, 0.88, 0, 0, 1, 1, '2026-06-26 23:33:32', '2026-07-26 23:33:32', NULL, 1, '满88享88折，适合加购饮品甜点', NULL, 1, 1, '2026-06-26 23:33:32', '2026-07-01 21:42:51', 0);

-- ----------------------------
-- Table structure for dining_table
-- ----------------------------
DROP TABLE IF EXISTS `dining_table`;
CREATE TABLE `dining_table`  (
  `id` bigint NOT NULL COMMENT '桌台ID',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '桌台编号（二维码关联）',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '桌台名称（如A1桌）',
  `capacity` int NOT NULL DEFAULT 4 COMMENT '座位数',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0空闲 1占用 2已结账 3待清洁）',
  `qr_code_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '二维码图片URL',
  `area_id` bigint NULL DEFAULT NULL COMMENT '区域ID',
  `area_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '区域名称（如大厅、包间）',
  `current_session_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '当前桌次编码',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_area_id`(`area_id` ASC) USING BTREE,
  INDEX `idx_current_session_code`(`current_session_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '桌台表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dining_table
-- ----------------------------
INSERT INTO `dining_table` VALUES (30001, 'A01', 'A1桌', 2, 0, 'table/qrcode/A01-fed19efe4ebb49c5b2a4c01fd696a3db.png', 31002, '大厅', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 18:50:17', 0);
INSERT INTO `dining_table` VALUES (30002, 'A02', 'A2桌', 2, 0, 'table/qrcode/A02-01042d5415ca46eb8ec91ccb3ebee22e.png', 31002, '大厅', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 00:44:28', 0);
INSERT INTO `dining_table` VALUES (30003, 'A03', 'A3桌', 4, 0, 'table/qrcode/A03-2d3230e48eda41b4a064f4b9b129bbcb.png', 31002, '大厅', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 18:50:23', 0);
INSERT INTO `dining_table` VALUES (30004, 'A04', 'A4桌', 4, 0, 'table/qrcode/A04-2552179a23794fbba794c24645c1e385.png', 31002, '大厅', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-04 15:56:00', 0);
INSERT INTO `dining_table` VALUES (30005, 'A05', 'A5桌', 4, 0, 'table/qrcode/A05-79ad6b9a6ee64152bfbcf88ed330806d.png', 31002, '大厅', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-04 16:14:20', 0);
INSERT INTO `dining_table` VALUES (30006, 'A06', 'A6桌', 6, 0, 'table/qrcode/A06-e2062ab56b7348edace00e0a70535926.png', 31002, '大厅', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 18:50:43', 0);
INSERT INTO `dining_table` VALUES (30007, 'A07', 'A7桌', 6, 0, 'table/qrcode/A07-62658241022646fab342003e5b12859b.png', 31002, '大厅', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 18:50:55', 0);
INSERT INTO `dining_table` VALUES (30008, 'A08', 'A8桌', 4, 0, 'table/qrcode/A08-b663ec73aac54c52bf3304ae012b6e10.png', 31002, '大厅', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 00:44:31', 0);
INSERT INTO `dining_table` VALUES (30009, 'A09', 'A9桌', 4, 0, 'table/qrcode/A09-2e3d7ef7166949a9a22be86fcdeb4fc5.png', 31002, '大厅', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 18:50:32', 0);
INSERT INTO `dining_table` VALUES (30010, 'A10', 'A10桌', 8, 0, 'table/qrcode/A10-60c45331dd8d4c9b98567d5f8a273a91.png', 31002, '大厅', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 00:44:33', 0);
INSERT INTO `dining_table` VALUES (30011, 'A11', 'A11桌', 4, 0, 'table/qrcode/A11-0f0c26f8458243bb96440d4d9723c7cb.png', 31002, '大厅', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 00:44:33', 0);
INSERT INTO `dining_table` VALUES (30012, 'A12', 'A12桌', 4, 0, 'table/qrcode/A12-741b8c426d4b4a2691982812343766d2.png', 31002, '大厅', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 00:44:34', 0);
INSERT INTO `dining_table` VALUES (30013, 'B01', '牡丹厅', 8, 0, 'table/qrcode/B01-adf64d1d43504ec4b952d070905d3068.png', 31001, '包间', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-04 16:19:58', 0);
INSERT INTO `dining_table` VALUES (30014, 'B02', '兰花厅', 8, 0, 'table/qrcode/B02-9baa7a50764b4aa1b2fb9b035680da6a.png', 31001, '包间', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-04 16:29:37', 0);
INSERT INTO `dining_table` VALUES (30015, 'B03', '梅花厅', 10, 0, 'table/qrcode/B03-dfd353f955bb4278807a1fc2b5a6ec44.png', 31001, '包间', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-04 16:31:30', 0);
INSERT INTO `dining_table` VALUES (30016, 'B04', '竹韵厅', 10, 0, 'table/qrcode/B04-f961500398594d11b164f3df521aec28.png', 31001, '包间', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 00:44:24', 0);
INSERT INTO `dining_table` VALUES (30017, 'B05', '荷花厅', 12, 0, 'table/qrcode/B05-ebd2964140ae4c5f99873350559da9bc.png', 31001, '包间', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 18:51:24', 0);
INSERT INTO `dining_table` VALUES (30018, 'B06', '菊花厅', 12, 0, 'table/qrcode/B06-f56be4b889a94048ab7f470045dce78d.png', 31001, '包间', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 18:51:09', 0);
INSERT INTO `dining_table` VALUES (30019, 'B07', '桂花厅', 16, 0, 'table/qrcode/B07-50134eec4a6b479c99d9fa4a59a61fea.png', 31001, '包间', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 00:44:26', 0);
INSERT INTO `dining_table` VALUES (30020, 'B08', '紫薇厅', 20, 0, 'table/qrcode/B08-8e4f968ecaf14f17897ac41ed69c7958.png', 31001, '包间', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 00:44:27', 0);
INSERT INTO `dining_table` VALUES (30021, 'C01', 'C1桌', 2, 0, 'table/qrcode/C01-a53af542944d4f18a224b5a8bc3d01b2.png', 31003, '露台', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 00:44:35', 0);
INSERT INTO `dining_table` VALUES (30022, 'C02', 'C2桌', 2, 0, 'table/qrcode/C02-b1b3f5dc1f104425aba30de46fa71dc0.png', 31003, '露台', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 00:44:35', 0);
INSERT INTO `dining_table` VALUES (30023, 'C03', 'C3桌', 4, 0, 'table/qrcode/C03-6f4bd894e9d94a61bb5fdc732fd7edf8.png', 31003, '露台', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 00:44:36', 0);
INSERT INTO `dining_table` VALUES (30024, 'C04', 'C4桌', 4, 0, 'table/qrcode/C04-7276118ac3aa49508eb9ccb1eef0feb4.png', 31003, '露台', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 00:44:36', 0);
INSERT INTO `dining_table` VALUES (30025, 'C05', 'C5桌', 6, 0, 'table/qrcode/C05-5a5cd46303394a17b39cfc47b31088eb.png', 31003, '露台', NULL, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 00:44:37', 0);

-- ----------------------------
-- Table structure for dish
-- ----------------------------
DROP TABLE IF EXISTS `dish`;
CREATE TABLE `dish`  (
  `id` bigint NOT NULL COMMENT '菜品ID',
  `category_id` bigint NOT NULL COMMENT '所属分类ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜品名称',
  `price` decimal(10, 2) NOT NULL COMMENT '价格',
  `image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图片URL',
  `thumbnail` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '缩略图URL',
  `spice_level` tinyint NOT NULL DEFAULT 0 COMMENT '辣度标记（0不辣 1微辣 2中辣 3重辣）',
  `spec_values` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '扩展规格值(JSON)',
  `ingredients` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '配料列表（JSON数组）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '简介',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0下架 1上架）',
  `sold_out` tinyint NOT NULL DEFAULT 0 COMMENT '是否售罄（0否 1是）',
  `stock` int NOT NULL DEFAULT -1 COMMENT '库存数量（-1表示不限库存）',
  `preparation_time` int NULL DEFAULT NULL COMMENT '预设制作时限（分钟）',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category_id`(`category_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜品表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dish
-- ----------------------------
INSERT INTO `dish` VALUES (20001, 10001, '老醋花生', 0.01, 'dish/00573c5882dd49048aa36451e30919c9.jpg', 'dish/00573c5882dd49048aa36451e30919c9.jpg', 0, NULL, '[\"花生米\",\"香醋\",\"香菜\"]', '酥脆花生配陈醋，开胃爽口', 1, 0, -1, 5, 1, 1, '2025-12-01 10:00:00', '2026-07-02 23:22:05', 0);
INSERT INTO `dish` VALUES (20002, 10001, '凉拌黄瓜', 0.01, 'dish/137a97acab3a462dbe1369a02a714e9d.jpg', 'dish/137a97acab3a462dbe1369a02a714e9d.jpg', 0, NULL, '[\"黄瓜\",\"蒜末\",\"辣椒油\"]', '清脆爽口，微辣开胃', 1, 0, -1, 3, 1, 1, '2025-12-01 10:00:00', '2026-07-02 23:22:11', 0);
INSERT INTO `dish` VALUES (20003, 10001, '皮蛋豆腐', 22.00, 'dish/1a4eda90a36e4335822e1326cb5b1bea.jpg', 'dish/1a4eda90a36e4335822e1326cb5b1bea.jpg', 0, NULL, '[\"皮蛋\",\"嫩豆腐\",\"酱油\"]', '经典凉菜，入口即化', 1, 0, -1, 5, 1, 1, '2025-12-01 10:00:00', '2026-03-02 21:45:16', 0);
INSERT INTO `dish` VALUES (20004, 10001, '口水鸡', 38.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 3, NULL, '[\"鸡肉\",\"花椒\",\"辣椒\"]', '麻辣鲜香，口水直流', 1, 0, -1, 10, 1, 1, '2025-12-01 10:00:00', '2026-03-02 21:45:28', 0);
INSERT INTO `dish` VALUES (20005, 10001, '蒜泥白肉', 32.00, 'dish/08853d5f385d4c26b29cb9c00a2c8820.jpg', 'dish/08853d5f385d4c26b29cb9c00a2c8820.jpg', 1, NULL, '[\"五花肉\",\"蒜泥\",\"酱油\"]', '薄如蝉翼，蒜香浓郁', 1, 0, -1, 8, 1, 1, '2025-12-01 10:00:00', '2026-06-27 16:04:07', 0);
INSERT INTO `dish` VALUES (20006, 10001, '夫妻肺片', 36.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 2, NULL, '[\"牛肉\",\"牛杂\",\"辣椒油\"]', '川味经典，麻辣鲜香', 1, 0, -1, 8, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20007, 10001, '凉拌木耳', 16.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 1, NULL, '[\"黑木耳\",\"青椒\",\"醋\"]', '爽脆可口，营养丰富', 1, 0, -1, 5, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20008, 10001, '糖醋排骨', 42.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"排骨\",\"糖\",\"醋\"]', '外酥里嫩，酸甜可口', 1, 0, 31, 15, 1, NULL, '2025-12-01 10:00:00', '2026-07-04 16:31:30', 0);
INSERT INTO `dish` VALUES (20101, 10002, '宫保鸡丁', 38.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 2, NULL, '[\"鸡胸肉\",\"花生\",\"干辣椒\"]', '经典川菜，香辣酥脆', 1, 0, -1, 12, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20102, 10002, '鱼香肉丝', 36.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 2, NULL, '[\"猪肉\",\"木耳\",\"胡萝卜\"]', '酸甜微辣，下饭神器', 1, 0, -1, 12, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20103, 10002, '麻婆豆腐', 28.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 3, NULL, '[\"豆腐\",\"牛肉末\",\"花椒\"]', '麻辣烫鲜，川菜之魂', 1, 0, -1, 10, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20104, 10002, '回锅肉', 42.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 2, NULL, '[\"五花肉\",\"蒜苗\",\"豆瓣酱\"]', '肥而不腻，酱香浓郁', 1, 0, -1, 15, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20105, 10002, '水煮牛肉', 58.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 3, NULL, '[\"牛肉\",\"豆芽\",\"辣椒\"]', '麻辣鲜烫，肉质嫩滑', 1, 0, -1, 18, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20106, 10002, '红烧肉', 48.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"五花肉\",\"冰糖\",\"酱油\"]', '色泽红亮，入口即化', 1, 0, -1, 25, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20107, 10002, '干煸四季豆', 26.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 2, NULL, '[\"四季豆\",\"肉末\",\"干辣椒\"]', '干香酥脆，下饭首选', 1, 0, -1, 10, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20108, 10002, '酸菜鱼', 68.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 2, NULL, '[\"草鱼\",\"酸菜\",\"泡椒\"]', '酸辣开胃，鱼肉鲜嫩', 1, 0, -1, 20, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20109, 10002, '铁板牛柳', 52.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 1, NULL, '[\"牛柳\",\"洋葱\",\"青椒\"]', '铁板滋滋作响，牛肉嫩滑', 1, 0, -1, 15, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20110, 10002, '地三鲜', 28.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"茄子\",\"土豆\",\"青椒\"]', '东北名菜，家常美味', 1, 0, -1, 12, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20111, 10002, '辣子鸡', 46.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 3, NULL, '[\"鸡块\",\"干辣椒\",\"花椒\"]', '香辣酥脆，越吃越香', 1, 0, -1, 15, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20112, 10002, '蚂蚁上树', 24.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 2, NULL, '[\"粉丝\",\"肉末\",\"豆瓣酱\"]', '粉丝入味，香辣可口', 1, 0, -1, 10, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20201, 10003, '番茄蛋花汤', 18.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"番茄\",\"鸡蛋\",\"香菜\"]', '酸甜可口，营养丰富', 1, 0, -1, 8, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20202, 10003, '酸辣汤', 22.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 2, NULL, '[\"豆腐\",\"木耳\",\"鸡蛋\"]', '酸辣开胃，暖身暖胃', 1, 0, -1, 10, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20203, 10003, '紫菜蛋花汤', 15.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"紫菜\",\"鸡蛋\",\"虾皮\"]', '清淡鲜美，简单美味', 1, 0, -1, 5, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20204, 10003, '排骨莲藕汤', 48.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"排骨\",\"莲藕\",\"枸杞\"]', '慢火炖煮，汤鲜味美', 1, 0, -1, 30, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20205, 10003, '冬瓜排骨汤', 38.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"冬瓜\",\"排骨\",\"姜片\"]', '清热解暑，鲜美可口', 1, 0, -1, 25, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20206, 10003, '鲫鱼豆腐汤', 42.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"鲫鱼\",\"豆腐\",\"葱姜\"]', '奶白浓汤，鲜美滋补', 1, 0, -1, 25, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20301, 10004, '蛋炒饭', 16.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"米饭\",\"鸡蛋\",\"葱花\"]', '粒粒分明，蛋香浓郁', 1, 0, -1, 8, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20302, 10004, '扬州炒饭', 22.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"米饭\",\"虾仁\",\"火腿\"]', '色彩丰富，口感层次分明', 1, 0, -1, 10, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20303, 10004, '担担面', 18.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 3, NULL, '[\"面条\",\"肉末\",\"花椒\"]', '麻辣鲜香，四川名面', 1, 0, -1, 8, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20304, 10004, '重庆小面', 16.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 3, NULL, '[\"面条\",\"花生\",\"辣椒\"]', '麻辣爽口，重庆味道', 1, 0, -1, 8, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20305, 10004, '葱油拌面', 14.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"面条\",\"葱油\",\"酱油\"]', '简单美味，葱香四溢', 1, 0, -1, 6, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20306, 10004, '牛肉面', 28.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 1, NULL, '[\"面条\",\"牛肉\",\"萝卜\"]', '汤浓面劲，牛肉大块', 1, 0, -1, 12, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20307, 10004, '白米饭', 3.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"大米\"]', '精选东北大米', 1, 0, -1, 2, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20308, 10004, '手抓饼', 12.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"面饼\",\"鸡蛋\",\"生菜\"]', '外酥里嫩，层次分明', 0, 0, -1, 8, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20401, 10005, '烤羊肉串', 8.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 2, NULL, '[\"羊肉\",\"孜然\",\"辣椒\"]', '外焦里嫩，孜然飘香', 1, 0, 199, 8, 1, NULL, '2025-12-01 10:00:00', '2026-07-02 18:42:41', 0);
INSERT INTO `dish` VALUES (20402, 10005, '烤鸡翅', 12.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 1, NULL, '[\"鸡翅\",\"蜂蜜\",\"酱油\"]', '蜜汁烤制，皮脆肉嫩', 1, 0, 99, 12, 1, NULL, '2025-12-01 10:00:00', '2026-07-02 18:42:41', 0);
INSERT INTO `dish` VALUES (20403, 10005, '烤茄子', 18.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 2, NULL, '[\"茄子\",\"蒜蓉\",\"辣椒\"]', '蒜香浓郁，软糯入味', 1, 0, -1, 15, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20404, 10005, '烤金针菇', 10.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 1, NULL, '[\"金针菇\",\"蒜蓉\",\"酱油\"]', '鲜嫩多汁，蒜香扑鼻', 1, 0, -1, 10, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20405, 10005, '烤五花肉', 15.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 1, NULL, '[\"五花肉\",\"孜然\",\"椒盐\"]', '肥瘦相间，焦香诱人', 1, 0, 148, 10, 1, NULL, '2025-12-01 10:00:00', '2026-03-03 16:48:46', 0);
INSERT INTO `dish` VALUES (20406, 10005, '烤玉米', 8.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"玉米\",\"黄油\",\"椒盐\"]', '甜糯可口，黄油飘香', 1, 0, -1, 12, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20407, 10005, '烤韭菜', 8.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 1, NULL, '[\"韭菜\",\"孜然\",\"辣椒\"]', '烧烤必点，香气四溢', 1, 0, -1, 8, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20408, 10005, '烤生蚝', 15.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 1, NULL, '[\"生蚝\",\"蒜蓉\",\"粉丝\"]', '蒜蓉烤制，鲜美多汁', 1, 0, 77, 10, 1, NULL, '2025-12-01 10:00:00', '2026-07-02 18:54:36', 0);
INSERT INTO `dish` VALUES (20501, 10006, '清蒸鲈鱼', 0.01, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"鲈鱼\",\"葱姜\",\"蒸鱼豉油\"]', '鲜嫩滑口，原汁原味', 1, 0, 11, 15, 1, NULL, '2025-12-01 10:00:00', '2026-06-26 21:34:28', 0);
INSERT INTO `dish` VALUES (20502, 10006, '蒜蓉粉丝蒸扇贝', 48.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"扇贝\",\"粉丝\",\"蒜蓉\"]', '蒜香浓郁，鲜美无比', 1, 0, 28, 12, 1, NULL, '2025-12-01 10:00:00', '2026-03-03 16:48:46', 0);
INSERT INTO `dish` VALUES (20503, 10006, '香辣蟹', 88.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 3, NULL, '[\"螃蟹\",\"干辣椒\",\"花椒\"]', '麻辣鲜香，蟹肉饱满', 1, 1, 15, 20, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20504, 10006, '白灼虾', 58.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"基围虾\",\"姜片\",\"酱油\"]', '虾肉Q弹，原味鲜甜', 1, 0, 22, 8, 1, NULL, '2025-12-01 10:00:00', '2026-07-03 23:29:25', 0);
INSERT INTO `dish` VALUES (20505, 10006, '椒盐皮皮虾', 78.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 1, NULL, '[\"皮皮虾\",\"椒盐\",\"蒜末\"]', '外酥里嫩，椒盐飘香', 1, 0, 14, 15, 1, NULL, '2025-12-01 10:00:00', '2026-07-04 00:41:40', 0);
INSERT INTO `dish` VALUES (20506, 10006, '蒜蓉小龙虾', 98.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 2, NULL, '[\"小龙虾\",\"蒜蓉\",\"啤酒\"]', '蒜香浓郁，虾肉鲜嫩', 1, 1, 30, 20, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20507, 10006, '爆炒花甲', 32.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 2, NULL, '[\"花甲\",\"姜蒜\",\"辣椒\"]', '鲜辣可口，下酒佳品', 1, 0, -1, 10, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20601, 10007, '鲜榨橙汁', 18.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"鲜橙\"]', '现榨现饮，维C满满', 1, 0, -1, 3, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20602, 10007, '柠檬蜂蜜水', 12.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"柠檬\",\"蜂蜜\"]', '酸甜可口，美容养颜', 1, 0, -1, 3, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20603, 10007, '酸梅汤', 10.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"乌梅\",\"山楂\",\"冰糖\"]', '古法熬制，消暑解腻', 1, 0, -1, 2, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20604, 10007, '可乐', 6.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"可口可乐\"]', '冰镇可乐，畅爽解渴', 1, 0, 199, 1, 1, NULL, '2025-12-01 10:00:00', '2026-07-02 18:46:39', 0);
INSERT INTO `dish` VALUES (20605, 10007, '雪碧', 6.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"雪碧\"]', '透心凉，心飞扬', 1, 0, 200, 1, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20606, 10007, '王老吉', 8.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"凉茶\"]', '怕上火，喝王老吉', 1, 0, 150, 1, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20607, 10007, '青岛啤酒', 10.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"啤酒\"]', '经典青岛，清爽畅饮', 1, 0, 300, 1, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20608, 10007, '百威啤酒', 15.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"啤酒\"]', '进口百威，醇厚顺滑', 1, 0, 199, 1, 1, NULL, '2025-12-01 10:00:00', '2026-03-03 16:50:18', 0);
INSERT INTO `dish` VALUES (20701, 10008, '红糖糍粑', 22.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"糯米\",\"红糖\",\"黄豆粉\"]', '外酥里糯，红糖飘香', 1, 0, -1, 8, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20702, 10008, '芒果西米露', 18.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"芒果\",\"西米\",\"椰浆\"]', '热带风情，甜蜜清凉', 1, 0, -1, 5, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20703, 10008, '双皮奶', 16.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"牛奶\",\"鸡蛋\",\"白糖\"]', '奶香浓郁，嫩滑如丝', 1, 0, -1, 10, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20704, 10008, '冰粉', 10.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"冰粉\",\"红糖\",\"花生\"]', '冰凉爽口，消暑必备', 1, 0, -1, 3, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20705, 10008, '杨枝甘露', 22.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"芒果\",\"西柚\",\"椰浆\"]', '港式经典，清甜可口', 1, 0, -1, 5, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20706, 10008, '拔丝地瓜', 24.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"地瓜\",\"白糖\"]', '金丝缠绕，甜而不腻', 1, 0, -1, 10, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20707, 10008, '银耳莲子羹', 15.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"银耳\",\"莲子\",\"枸杞\"]', '滋阴润肺，甜蜜养生', 1, 0, -1, 20, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (20708, 10008, '炸鲜奶', 18.00, 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 'dish/b7a59d8fc4f444d7ab566ea6685ec988.jpg', 0, NULL, '[\"牛奶\",\"面包糠\",\"白糖\"]', '外酥里嫩，奶香四溢', 1, 0, -1, 8, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 21:47:18', 0);
INSERT INTO `dish` VALUES (2072243174264893442, 10007, '芒果耶耶', 0.01, 'dish/a10c05af7ba84c6aae5f33aad3c57ed5.jpg', 'dish/a10c05af7ba84c6aae5f33aad3c57ed5.jpg', 0, NULL, NULL, '清爽可口', 1, 0, 92, 10, 1, 1, '2026-07-01 16:57:28', '2026-07-04 16:20:27', 0);
INSERT INTO `dish` VALUES (2072263042339004418, 10007, '杨枝甘露', 0.01, 'dish/e8f2b59b4ddf4e32af5e694e59a2400d.jpg', 'dish/e8f2b59b4ddf4e32af5e694e59a2400d.jpg', 0, NULL, NULL, NULL, 1, 0, 94, 10, 1, 1, '2026-07-01 18:16:25', '2026-07-04 16:40:18', 0);
INSERT INTO `dish` VALUES (2072998116231606274, 2072242597531316225, '柳州螺蛳粉', 15.00, 'dish/79cf34d39b97487089630fa298240ee1.jpg', 'dish/79cf34d39b97487089630fa298240ee1.jpg', 0, NULL, NULL, '广西柳州特色美食，超级好吃', 1, 0, 90, 15, 1, 1, '2026-07-03 18:57:20', '2026-07-04 16:31:54', 0);

-- ----------------------------
-- Table structure for dish_category
-- ----------------------------
DROP TABLE IF EXISTS `dish_category`;
CREATE TABLE `dish_category`  (
  `id` bigint NOT NULL COMMENT '分类ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名称',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序序号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0停用 1启用）',
  `image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分类图片',
  `spec_template` tinyint NOT NULL DEFAULT 1 COMMENT '规格模板（0无规格 1辣度 2饮品规格）',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜品分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dish_category
-- ----------------------------
INSERT INTO `dish_category` VALUES (10001, '凉菜', 1, 1, 'dish/da7af074b69e4d9db633ceb06cfdb63a.jpg', 1, 1, 1, '2025-12-01 10:00:00', '2026-07-03 19:43:18', 0);
INSERT INTO `dish_category` VALUES (10002, '热菜', 2, 1, 'dish/c5bce648113e4c60a378b752422d5154.jpg', 1, 1, 1, '2025-12-01 10:00:00', '2026-07-03 19:19:51', 0);
INSERT INTO `dish_category` VALUES (10003, '汤品', 3, 1, 'dish/81008b84949c4bcf92d3342b2729ae14.jpg', 1, 1, 1, '2025-12-01 10:00:00', '2026-07-03 19:43:01', 0);
INSERT INTO `dish_category` VALUES (10004, '主食', 4, 1, 'dish/5009da4f2ca9490eb0528dea36ea0b78.jpg', 1, 1, 1, '2025-12-01 10:00:00', '2026-07-03 19:42:54', 0);
INSERT INTO `dish_category` VALUES (10005, '烧烤', 5, 1, 'dish/c29ffb80860e4063ab4403bb042a224c.jpg', 1, 1, 1, '2025-12-01 10:00:00', '2026-07-03 19:42:45', 0);
INSERT INTO `dish_category` VALUES (10006, '海鲜', 6, 1, 'dish/bf9abd35c2a249379b82f62b79567c91.jpg', 1, 1, 1, '2025-12-01 10:00:00', '2026-07-03 19:24:35', 0);
INSERT INTO `dish_category` VALUES (10007, '饮品', 7, 1, 'dish/55e0dc3eb9774c7ebf728e47338843ad.jpg', 2, 1, 1, '2025-12-01 10:00:00', '2026-07-03 19:47:25', 0);
INSERT INTO `dish_category` VALUES (10008, '甜品', 8, 1, 'dish/cddd52d8487f406d9718d9136831849d.jpg', 1, 1, 1, '2025-12-01 10:00:00', '2026-07-03 19:42:25', 0);
INSERT INTO `dish_category` VALUES (2072242597531316225, '桂菜', 9, 1, 'dish/dcaa08ee2794407f9234b080e3d7495f.jpg', 1, 1, 1, '2026-07-01 16:55:10', '2026-07-03 19:42:16', 0);
INSERT INTO `dish_category` VALUES (2073003160104419330, '粤菜', 10, 1, 'dish/ff5b2f8a31a2497899dbb88ea8a150de.jpg', 1, 1, 1, '2026-07-03 19:17:23', '2026-07-03 19:17:23', 0);

-- ----------------------------
-- Table structure for dish_category_spec
-- ----------------------------
DROP TABLE IF EXISTS `dish_category_spec`;
CREATE TABLE `dish_category_spec`  (
  `id` bigint NOT NULL COMMENT '关联ID',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `spec_group_id` bigint NOT NULL COMMENT '规格组ID',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_dish_category_spec`(`category_id` ASC, `spec_group_id` ASC) USING BTREE,
  INDEX `idx_dish_category_spec_group_id`(`spec_group_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '分类默认规格关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dish_category_spec
-- ----------------------------
INSERT INTO `dish_category_spec` VALUES (2073003160171528194, 2073003160104419330, 31001, 1, 1, '2026-07-03 19:17:23', '2026-07-03 19:17:23', 0);
INSERT INTO `dish_category_spec` VALUES (2073003160171528195, 2073003160104419330, 31002, 1, 1, '2026-07-03 19:17:23', '2026-07-03 19:17:23', 0);
INSERT INTO `dish_category_spec` VALUES (2073003160171528196, 2073003160104419330, 31003, 1, 1, '2026-07-03 19:17:23', '2026-07-03 19:17:23', 0);
INSERT INTO `dish_category_spec` VALUES (2073004975218769921, 10006, 31001, 1, 1, '2026-07-03 19:24:35', '2026-07-03 19:24:35', 0);
INSERT INTO `dish_category_spec` VALUES (2073009421990612993, 2072242597531316225, 31001, 1, 1, '2026-07-03 19:42:16', '2026-07-03 19:42:16', 0);
INSERT INTO `dish_category_spec` VALUES (2073009459835817986, 10008, 31002, 1, 1, '2026-07-03 19:42:25', '2026-07-03 19:42:25', 0);
INSERT INTO `dish_category_spec` VALUES (2073009459835817987, 10008, 31003, 1, 1, '2026-07-03 19:42:25', '2026-07-03 19:42:25', 0);
INSERT INTO `dish_category_spec` VALUES (2073009544023887873, 10005, 31001, 1, 1, '2026-07-03 19:42:45', '2026-07-03 19:42:45', 0);
INSERT INTO `dish_category_spec` VALUES (2073009612185522177, 10003, 31002, 1, 1, '2026-07-03 19:43:01', '2026-07-03 19:43:01', 0);
INSERT INTO `dish_category_spec` VALUES (2073009612248436738, 10003, 31003, 1, 1, '2026-07-03 19:43:01', '2026-07-03 19:43:01', 0);
INSERT INTO `dish_category_spec` VALUES (2073009684444991489, 10001, 31001, 1, 1, '2026-07-03 19:43:18', '2026-07-03 19:43:18', 0);
INSERT INTO `dish_category_spec` VALUES (2073010721528602625, 10007, 31002, 1, 1, '2026-07-03 19:47:25', '2026-07-03 19:47:25', 0);
INSERT INTO `dish_category_spec` VALUES (2073010721528602626, 10007, 31003, 1, 1, '2026-07-03 19:47:25', '2026-07-03 19:47:25', 0);

-- ----------------------------
-- Table structure for dish_spec_group
-- ----------------------------
DROP TABLE IF EXISTS `dish_spec_group`;
CREATE TABLE `dish_spec_group`  (
  `id` bigint NOT NULL COMMENT '规格组ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规格组名称',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序序号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0停用 1启用）',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜品规格组表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dish_spec_group
-- ----------------------------
INSERT INTO `dish_spec_group` VALUES (31001, '辣度', 1, 1, 1, 1, '2026-07-01 17:50:32', '2026-07-01 17:50:32', 0);
INSERT INTO `dish_spec_group` VALUES (31002, '甜度', 2, 1, 1, 1, '2026-07-01 17:50:41', '2026-07-01 17:50:41', 0);
INSERT INTO `dish_spec_group` VALUES (31003, '温度', 3, 1, 1, 1, '2026-07-01 17:50:50', '2026-07-01 17:50:50', 0);

-- ----------------------------
-- Table structure for dish_spec_mapping
-- ----------------------------
DROP TABLE IF EXISTS `dish_spec_mapping`;
CREATE TABLE `dish_spec_mapping`  (
  `id` bigint NOT NULL COMMENT '映射ID',
  `dish_id` bigint NOT NULL COMMENT '菜品ID',
  `spec_group_id` bigint NOT NULL COMMENT '规格组ID',
  `option_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规格值ID列表，逗号分隔',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_dish_spec_mapping_dish_id`(`dish_id` ASC) USING BTREE,
  INDEX `idx_dish_spec_mapping_group_id`(`spec_group_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜品规格映射表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dish_spec_mapping
-- ----------------------------
INSERT INTO `dish_spec_mapping` VALUES (2072263042401918978, 2072263042339004418, 31002, '31201,31202,31203,31204', 1, 1, '2026-07-01 18:16:25', '2026-07-01 18:33:16', 1);
INSERT INTO `dish_spec_mapping` VALUES (2072263042469027842, 2072263042339004418, 31003, '31301,31302,31303,31304', 1, 1, '2026-07-01 18:16:25', '2026-07-01 18:33:16', 1);
INSERT INTO `dish_spec_mapping` VALUES (2072267287893204993, 2072263042339004418, 31002, '31201,31202,31203,31204', 1, 1, '2026-07-01 18:33:17', '2026-07-01 18:33:17', 0);
INSERT INTO `dish_spec_mapping` VALUES (2072267287922565122, 2072263042339004418, 31003, '31301,31302,31303,31304', 1, 1, '2026-07-01 18:33:17', '2026-07-01 18:33:17', 0);
INSERT INTO `dish_spec_mapping` VALUES (2072267379807182850, 2072243174264893442, 31002, '31201,31202,31203,31204', 1, 1, '2026-07-01 18:33:39', '2026-07-01 18:33:39', 0);
INSERT INTO `dish_spec_mapping` VALUES (2072267379836542977, 2072243174264893442, 31003, '31301,31302,31303,31304', 1, 1, '2026-07-01 18:33:39', '2026-07-01 18:33:39', 0);
INSERT INTO `dish_spec_mapping` VALUES (2072998116231606275, 2072998116231606274, 31001, '31101,31102,31103,31104', 1, 1, '2026-07-03 18:57:20', '2026-07-03 18:57:20', 0);

-- ----------------------------
-- Table structure for dish_spec_option
-- ----------------------------
DROP TABLE IF EXISTS `dish_spec_option`;
CREATE TABLE `dish_spec_option`  (
  `id` bigint NOT NULL COMMENT '规格选项ID',
  `group_id` bigint NOT NULL COMMENT '规格组ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '选项名称',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序序号',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_dish_spec_option_group_id`(`group_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜品规格选项表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dish_spec_option
-- ----------------------------
INSERT INTO `dish_spec_option` VALUES (31101, 31001, '不辣', 1, 1, 1, '2026-07-01 17:50:58', '2026-07-01 17:50:58', 0);
INSERT INTO `dish_spec_option` VALUES (31102, 31001, '微辣', 2, 1, 1, '2026-07-01 17:51:06', '2026-07-01 17:51:06', 0);
INSERT INTO `dish_spec_option` VALUES (31103, 31001, '中辣', 3, 1, 1, '2026-07-01 17:51:14', '2026-07-01 17:51:14', 0);
INSERT INTO `dish_spec_option` VALUES (31104, 31001, '重辣', 4, 1, 1, '2026-07-01 17:51:22', '2026-07-01 17:51:22', 0);
INSERT INTO `dish_spec_option` VALUES (31201, 31002, '无糖', 1, 1, 1, '2026-07-01 17:51:30', '2026-07-01 17:51:30', 0);
INSERT INTO `dish_spec_option` VALUES (31202, 31002, '三分糖', 2, 1, 1, '2026-07-01 17:51:42', '2026-07-01 17:51:42', 0);
INSERT INTO `dish_spec_option` VALUES (31203, 31002, '半糖', 3, 1, 1, '2026-07-01 17:51:51', '2026-07-01 17:51:51', 0);
INSERT INTO `dish_spec_option` VALUES (31204, 31002, '全糖', 4, 1, 1, '2026-07-01 17:52:05', '2026-07-01 17:52:05', 0);
INSERT INTO `dish_spec_option` VALUES (31301, 31003, '冰', 1, 1, 1, '2026-07-01 17:52:15', '2026-07-01 17:52:15', 0);
INSERT INTO `dish_spec_option` VALUES (31302, 31003, '少冰', 2, 1, 1, '2026-07-01 17:52:25', '2026-07-01 17:52:25', 0);
INSERT INTO `dish_spec_option` VALUES (31303, 31003, '常温', 3, 1, 1, '2026-07-01 17:52:34', '2026-07-01 17:52:34', 0);
INSERT INTO `dish_spec_option` VALUES (31304, 31003, '热', 4, 1, 1, '2026-07-01 17:52:46', '2026-07-01 17:52:46', 0);

-- ----------------------------
-- Table structure for home_banner
-- ----------------------------
DROP TABLE IF EXISTS `home_banner`;
CREATE TABLE `home_banner`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主标题',
  `subtitle` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '副标题',
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片地址或对象键',
  `action_type` tinyint NOT NULL DEFAULT 0 COMMENT '操作类型（0无动作 1页面跳转 2切换Tab）',
  `target_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '跳转路径',
  `scene` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'HOME' COMMENT '投放位置（HOME首页 MENU_HERO点餐页头图 PROFILE_HERO我的页头图）',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0停用 1启用）',
  `create_by` bigint NULL DEFAULT NULL,
  `update_by` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_home_banner_scene_status_sort`(`scene` ASC, `status` ASC, `sort` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '小程序轮播图表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of home_banner
-- ----------------------------
INSERT INTO `home_banner` VALUES (2070529130667544578, '今日招牌推荐', '现点现做，人气主菜正在热卖', 'dish/a8200af054cf44b7a7c9f19006cfaccf.jpg', 0, '', 'HOME', 1, 1, 1, 1, '2026-06-26 23:26:28', '2026-06-26 23:30:47', 0);
INSERT INTO `home_banner` VALUES (2070529230752026625, '热销必点清单', '经典热菜、凉菜搭配更省心', 'dish/b447369fc0ba476f84a5ed5397a81b6c.jpg', 0, '', 'HOME', 2, 1, 1, 1, '2026-06-26 23:26:52', '2026-06-26 23:30:47', 0);
INSERT INTO `home_banner` VALUES (2070529297214967809, '到店福利专区', '新客优惠券、限时活动到首页就能看', 'dish/2005a32637824a52bcae014d59a9b72a.jpg', 0, '', 'HOME', 3, 1, 1, 1, '2026-06-26 23:27:08', '2026-06-26 23:30:47', 0);
INSERT INTO `home_banner` VALUES (2070529366148354049, '饮品甜点上新', '正餐之后，再来一杯更舒服', 'dish/b4767478159745f5841aced71cb6a56a.jpg', 0, '', 'HOME', 4, 1, 1, 1, '2026-06-26 23:27:24', '2026-06-26 23:30:47', 0);

-- ----------------------------
-- Table structure for member_benefit_grant_log
-- ----------------------------
DROP TABLE IF EXISTS `member_benefit_grant_log`;
CREATE TABLE `member_benefit_grant_log`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `member_id` bigint NOT NULL COMMENT '会员ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `level_id` bigint NULL DEFAULT NULL COMMENT '等级ID',
  `template_id` bigint NULL DEFAULT NULL COMMENT '优惠券模板ID',
  `user_coupon_id` bigint NULL DEFAULT NULL COMMENT '用户优惠券ID',
  `benefit_type` tinyint NOT NULL COMMENT '权益类型：1生日权益 2升级礼包 3等级专属券 4积分兑换',
  `trigger_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发键',
  `trigger_value` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发值',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_member_benefit_grant_log_member_id`(`member_id` ASC) USING BTREE,
  INDEX `idx_member_benefit_grant_log_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_member_benefit_grant_log_benefit_trigger`(`benefit_type` ASC, `trigger_key` ASC, `trigger_value` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员权益发放日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of member_benefit_grant_log
-- ----------------------------

-- ----------------------------
-- Table structure for member_coupon_exchange
-- ----------------------------
DROP TABLE IF EXISTS `member_coupon_exchange`;
CREATE TABLE `member_coupon_exchange`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `template_id` bigint NOT NULL COMMENT '优惠券模板ID',
  `template_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '优惠券模板名称快照',
  `points_cost` int NOT NULL DEFAULT 0 COMMENT '兑换所需积分',
  `per_user_limit` int NOT NULL DEFAULT 0 COMMENT '每人兑换次数上限，0不限',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0停用 1启用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_member_coupon_exchange_template_id`(`template_id` ASC) USING BTREE,
  INDEX `idx_member_coupon_exchange_status_sort`(`status` ASC, `sort` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员积分兑换优惠券配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of member_coupon_exchange
-- ----------------------------

-- ----------------------------
-- Table structure for member_growth_record
-- ----------------------------
DROP TABLE IF EXISTS `member_growth_record`;
CREATE TABLE `member_growth_record`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `member_id` bigint NOT NULL COMMENT '会员ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `biz_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务类型',
  `biz_id` bigint NOT NULL COMMENT '业务ID',
  `change_amount` int NOT NULL COMMENT '成长值变动',
  `growth_after` int NOT NULL COMMENT '变动后成长值',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_member_growth_record_biz`(`biz_type` ASC, `biz_id` ASC) USING BTREE,
  INDEX `idx_member_growth_record_member_id`(`member_id` ASC) USING BTREE,
  INDEX `idx_member_growth_record_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_member_growth_record_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员模块-成长值流水表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of member_growth_record
-- ----------------------------

-- ----------------------------
-- Table structure for member_level
-- ----------------------------
DROP TABLE IF EXISTS `member_level`;
CREATE TABLE `member_level`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `level_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '等级编码',
  `level_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '等级名称',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `growth_threshold` int NOT NULL DEFAULT 0 COMMENT '成长值门槛',
  `points_rate` decimal(5, 2) NOT NULL DEFAULT 1.00 COMMENT '积分倍率',
  `discount_rate` decimal(5, 2) NOT NULL DEFAULT 1.00 COMMENT '折扣倍率',
  `benefit_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '权益配置JSON',
  `upgrade_coupon_template_id` bigint NULL DEFAULT NULL COMMENT '升级礼包优惠券模板ID',
  `exclusive_coupon_template_id` bigint NULL DEFAULT NULL COMMENT '等级专属优惠券模板ID',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0禁用 1启用）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_member_level_code`(`level_code` ASC) USING BTREE,
  INDEX `idx_member_level_sort`(`sort` ASC) USING BTREE,
  INDEX `idx_member_level_growth_threshold`(`growth_threshold` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员模块-会员等级表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of member_level
-- ----------------------------
INSERT INTO `member_level` VALUES (2001, 'NORMAL', '普通会员', 1, 0, 1.00, 1.00, NULL, NULL, NULL, 1, '默认会员等级', 1, 1, '2026-06-30 12:47:12', '2026-06-30 12:47:12', 0);
INSERT INTO `member_level` VALUES (2002, 'SILVER', '银卡会员', 2, 1000, 1.10, 1.00, NULL, NULL, NULL, 1, '成长值满1000升级', 1, 1, '2026-06-30 12:47:12', '2026-06-30 12:47:12', 0);
INSERT INTO `member_level` VALUES (2003, 'GOLD', '金卡会员', 3, 5000, 1.20, 0.98, NULL, NULL, NULL, 1, '成长值满5000升级', 1, 1, '2026-06-30 12:47:12', '2026-06-30 12:47:12', 0);
INSERT INTO `member_level` VALUES (2004, 'BLACK_GOLD', '黑金会员', 4, 15000, 1.50, 0.95, NULL, NULL, NULL, 1, '成长值满15000升级', 1, 1, '2026-06-30 12:47:12', '2026-06-30 12:47:12', 0);

-- ----------------------------
-- Table structure for member_level_change_log
-- ----------------------------
DROP TABLE IF EXISTS `member_level_change_log`;
CREATE TABLE `member_level_change_log`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `member_id` bigint NOT NULL COMMENT '会员ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `old_level_id` bigint NULL DEFAULT NULL COMMENT '原等级ID',
  `new_level_id` bigint NOT NULL COMMENT '新等级ID',
  `change_reason` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '变更原因',
  `biz_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务类型',
  `biz_id` bigint NULL DEFAULT NULL COMMENT '业务ID',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_member_level_change_log_member_id`(`member_id` ASC) USING BTREE,
  INDEX `idx_member_level_change_log_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_member_level_change_log_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员模块-等级变更日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of member_level_change_log
-- ----------------------------

-- ----------------------------
-- Table structure for member_points_record
-- ----------------------------
DROP TABLE IF EXISTS `member_points_record`;
CREATE TABLE `member_points_record`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `member_id` bigint NOT NULL COMMENT '会员ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `change_type` tinyint NOT NULL COMMENT '变动类型',
  `biz_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务类型',
  `biz_id` bigint NOT NULL COMMENT '业务ID',
  `change_amount` int NOT NULL COMMENT '变动积分',
  `balance_after` int NOT NULL COMMENT '变动后余额',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_member_points_record_biz`(`biz_type` ASC, `biz_id` ASC) USING BTREE,
  INDEX `idx_member_points_record_member_id`(`member_id` ASC) USING BTREE,
  INDEX `idx_member_points_record_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_member_points_record_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员模块-积分流水表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of member_points_record
-- ----------------------------

-- ----------------------------
-- Table structure for member_profile
-- ----------------------------
DROP TABLE IF EXISTS `member_profile`;
CREATE TABLE `member_profile`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `member_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会员编号',
  `level_id` bigint NOT NULL COMMENT '当前等级ID',
  `growth_value` int NOT NULL DEFAULT 0 COMMENT '当前成长值',
  `points_balance` int NOT NULL DEFAULT 0 COMMENT '当前积分余额',
  `total_points_earned` int NOT NULL DEFAULT 0 COMMENT '累计获得积分',
  `total_points_used` int NOT NULL DEFAULT 0 COMMENT '累计使用积分',
  `total_amount_consumed` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '累计消费金额',
  `birthday` date NULL DEFAULT NULL COMMENT '生日',
  `register_source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '注册来源',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0冻结 1正常）',
  `last_consume_time` datetime NULL DEFAULT NULL COMMENT '最后消费时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_member_profile_user_id`(`user_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_member_profile_member_no`(`member_no` ASC) USING BTREE,
  INDEX `idx_member_profile_level_id`(`level_id` ASC) USING BTREE,
  INDEX `idx_member_profile_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员模块-会员档案表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of member_profile
-- ----------------------------

-- ----------------------------
-- Table structure for mq_consume_log
-- ----------------------------
DROP TABLE IF EXISTS `mq_consume_log`;
CREATE TABLE `mq_consume_log`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `consumer_group` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消费者组',
  `topic` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息主题',
  `tag` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '消息标签',
  `message_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息唯一键',
  `biz_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务主键',
  `consume_status` tinyint NOT NULL DEFAULT 0 COMMENT '消费状态（0处理中 1成功 2失败）',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '消费重试次数',
  `last_error` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后错误信息',
  `finished_time` datetime NULL DEFAULT NULL COMMENT '消费完成时间',
  `create_by` bigint NULL DEFAULT NULL,
  `update_by` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_mq_consume_message`(`consumer_group` ASC, `message_key` ASC) USING BTREE,
  INDEX `idx_mq_consume_biz`(`biz_key` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '消息消费幂等日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mq_consume_log
-- ----------------------------

-- ----------------------------
-- Table structure for mq_message
-- ----------------------------
DROP TABLE IF EXISTS `mq_message`;
CREATE TABLE `mq_message`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `message_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息唯一键',
  `topic` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息主题',
  `tag` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '消息标签',
  `biz_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型',
  `biz_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务主键',
  `payload` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息体',
  `deliver_status` tinyint NOT NULL DEFAULT 0 COMMENT '投递状态（0待投递 1投递中 2已投递 3投递失败 4已死亡）',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `next_retry_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次重试时间',
  `last_error` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后错误信息',
  `sent_time` datetime NULL DEFAULT NULL COMMENT '发送成功时间',
  `create_by` bigint NULL DEFAULT NULL,
  `update_by` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_mq_message_key`(`message_key` ASC) USING BTREE,
  INDEX `idx_mq_message_status_retry`(`deliver_status` ASC, `next_retry_time` ASC) USING BTREE,
  INDEX `idx_mq_message_biz`(`biz_type` ASC, `biz_key` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '可靠消息出站表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mq_message
-- ----------------------------

-- ----------------------------
-- Table structure for order
-- ----------------------------
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order`  (
  `id` bigint NOT NULL COMMENT '订单ID',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单编号',
  `table_id` bigint NULL DEFAULT NULL COMMENT '关联桌台ID',
  `table_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '桌台编号（冗余）',
  `table_session_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '桌次编码（冗余）',
  `original_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '原始总金额',
  `discount_rate` decimal(3, 2) NOT NULL DEFAULT 1.00 COMMENT '折扣比例（默认1.00）',
  `coupon_id` bigint NULL DEFAULT NULL COMMENT '优惠券ID',
  `coupon_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '优惠券名称',
  `coupon_type` tinyint NULL DEFAULT NULL COMMENT '优惠券类型（1满减 2折扣）',
  `coupon_threshold_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '优惠券门槛金额',
  `coupon_discount_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '优惠券减免金额',
  `coupon_discount_rate` decimal(4, 2) NULL DEFAULT NULL COMMENT '优惠券折扣比例',
  `actual_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '实付总金额',
  `points_used` int NOT NULL DEFAULT 0 COMMENT '使用积分',
  `points_discount_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '积分抵现金额',
  `paid_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '已支付金额（AA场景）',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0待支付 1已支付 2已取消）',
  `payment_mode` tinyint NOT NULL DEFAULT 1 COMMENT '支付模式（0餐前付 1餐后付）',
  `order_type` tinyint NOT NULL DEFAULT 0 COMMENT '订单类型（0堂食 1外卖）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '订单备注',
  `customer_openid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '顾客微信openid',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_table_id`(`table_id` ASC) USING BTREE,
  INDEX `idx_table_session_status`(`table_id` ASC, `table_session_code` ASC, `status` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_customer_openid`(`customer_openid` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of order
-- ----------------------------

-- ----------------------------
-- Table structure for order_item
-- ----------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item`  (
  `id` bigint NOT NULL COMMENT '订单项ID',
  `order_id` bigint NOT NULL COMMENT '所属订单ID',
  `dish_id` bigint NOT NULL COMMENT '菜品ID',
  `dish_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜品名称（冗余）',
  `dish_image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜品图片（冗余）',
  `price` decimal(10, 2) NOT NULL COMMENT '下单时单价（冗余）',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `amount` decimal(10, 2) NOT NULL COMMENT '小计金额',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '口味备注',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0待制作 1制作中 2已完成）',
  `payment_status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态（0未支付 2已支付）',
  `is_gift` tinyint NOT NULL DEFAULT 0 COMMENT '是否赠送（0否 1是）',
  `added_at` datetime NULL DEFAULT NULL COMMENT '加入订单时间（用于区分加菜）',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_dish_id`(`dish_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '订单项表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of order_item
-- ----------------------------

-- ----------------------------
-- Table structure for order_item_review
-- ----------------------------
DROP TABLE IF EXISTS `order_item_review`;
CREATE TABLE `order_item_review`  (
  `id` bigint NOT NULL COMMENT '订单项评价ID',
  `review_id` bigint NOT NULL COMMENT '关联评价ID',
  `order_item_id` bigint NOT NULL COMMENT '关联订单项ID',
  `rating` tinyint NOT NULL COMMENT '评分（1-5）',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_review_id`(`review_id` ASC) USING BTREE,
  INDEX `idx_order_item_id`(`order_item_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '订单项评价表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of order_item_review
-- ----------------------------

-- ----------------------------
-- Table structure for order_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `order_operation_log`;
CREATE TABLE `order_operation_log`  (
  `id` bigint NOT NULL COMMENT '日志ID',
  `order_id` bigint NOT NULL COMMENT '关联订单ID',
  `order_item_id` bigint NULL DEFAULT NULL COMMENT '关联订单项ID',
  `operation_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作类型（RETURN/REPLACE/GIFT/DISCOUNT/RUSH）',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作人姓名',
  `reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作原因',
  `detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '操作详情（JSON）',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_order_item_id`(`order_item_id` ASC) USING BTREE,
  INDEX `idx_operator_id`(`operator_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '订单操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of order_operation_log
-- ----------------------------

-- ----------------------------
-- Table structure for order_review
-- ----------------------------
DROP TABLE IF EXISTS `order_review`;
CREATE TABLE `order_review`  (
  `id` bigint NOT NULL COMMENT '评价ID',
  `order_id` bigint NOT NULL COMMENT '关联订单ID',
  `overall_rating` tinyint NOT NULL COMMENT '总体评分（1-5）',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文字评价',
  `customer_openid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '评价人openid',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_id`(`order_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '订单评价表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of order_review
-- ----------------------------

-- ----------------------------
-- Table structure for payment_record
-- ----------------------------
DROP TABLE IF EXISTS `payment_record`;
CREATE TABLE `payment_record`  (
  `id` bigint NOT NULL COMMENT '支付记录ID',
  `order_id` bigint NOT NULL COMMENT '关联订单ID',
  `payment_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '支付流水号',
  `third_party_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '第三方支付流水号',
  `payment_method` tinyint NOT NULL COMMENT '支付方式（0微信 1支付宝 2现金 3会员卡）',
  `amount` decimal(10, 2) NOT NULL COMMENT '支付金额',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0待支付 1已支付 2待确认 3已退款）',
  `payer_openid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '支付人openid（AA场景）',
  `callback_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '支付回调原始数据',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_payment_no`(`payment_no` ASC) USING BTREE,
  INDEX `idx_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '支付记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of payment_record
-- ----------------------------

-- ----------------------------
-- Table structure for printer
-- ----------------------------
DROP TABLE IF EXISTS `printer`;
CREATE TABLE `printer`  (
  `id` bigint NOT NULL COMMENT '打印机ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '打印机名称',
  `sn` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '打印机序列号',
  `type` tinyint NOT NULL DEFAULT 0 COMMENT '类型（0前台 1后厨）',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0离线 1在线）',
  `location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '位置描述',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '打印机表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of printer
-- ----------------------------
INSERT INTO `printer` VALUES (40001, '前台收银打印机', 'SN-FRONT-001', 0, 0, '前台收银台', 1, 1, '2025-12-01 10:00:00', '2026-03-04 17:32:51', 0);
INSERT INTO `printer` VALUES (40002, '后厨热菜打印机', 'SN-KITCHEN-001', 1, 1, '后厨热菜区', 1, NULL, '2025-12-01 10:00:00', '2026-03-02 18:12:56', 0);
INSERT INTO `printer` VALUES (40003, '后厨凉菜打印机', 'SN-KITCHEN-002', 1, 1, '后厨凉菜区', 1, NULL, '2025-12-01 10:00:00', '2026-03-02 18:12:56', 0);
INSERT INTO `printer` VALUES (40004, '烧烤区打印机', 'SN-KITCHEN-003', 1, 1, '烧烤区', 1, 1, '2025-12-01 10:00:00', '2026-03-03 17:09:16', 0);

-- ----------------------------
-- Table structure for printer_category_mapping
-- ----------------------------
DROP TABLE IF EXISTS `printer_category_mapping`;
CREATE TABLE `printer_category_mapping`  (
  `id` bigint NOT NULL COMMENT '映射ID',
  `printer_id` bigint NOT NULL COMMENT '打印机ID',
  `category_id` bigint NOT NULL COMMENT '菜品分类ID',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_printer_category`(`printer_id` ASC, `category_id` ASC) USING BTREE,
  INDEX `idx_category_id`(`category_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '打印机-分类映射表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of printer_category_mapping
-- ----------------------------
INSERT INTO `printer_category_mapping` VALUES (50001, 40002, 10002, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 18:12:56', 0);
INSERT INTO `printer_category_mapping` VALUES (50002, 40002, 10003, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 18:12:56', 0);
INSERT INTO `printer_category_mapping` VALUES (50003, 40002, 10006, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 18:12:56', 0);
INSERT INTO `printer_category_mapping` VALUES (50004, 40003, 10001, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 18:12:56', 0);
INSERT INTO `printer_category_mapping` VALUES (50005, 40003, 10007, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 18:12:56', 0);
INSERT INTO `printer_category_mapping` VALUES (50006, 40003, 10008, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 18:12:56', 0);
INSERT INTO `printer_category_mapping` VALUES (50007, 40004, 10005, 1, NULL, '2025-12-01 10:00:00', '2026-03-02 18:12:56', 0);

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` bigint NOT NULL COMMENT '配置ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置名称',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置键',
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '配置值',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES (1, '系统名称', 'sys.name', '后端脚手架', '系统名称', NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_config` VALUES (2, '系统版本', 'sys.version', '1.0.0', '系统版本号', NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_config` VALUES (2070487983681265666, '后厨自动接单', 'kitchen.autoAccept', 'true', '控制新堂食订单是否自动接单', 1, 1, '2026-06-26 20:42:58', '2026-06-26 20:42:58', 0);
INSERT INTO `sys_config` VALUES (2072593941760950274, '管理端主题预设', 'admin.theme.preset', 'obsidian-night', '当前管理端全局主题预设ID', 1, 1, '2026-07-02 16:11:17', '2026-07-02 16:11:17', 0);

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data`  (
  `id` bigint NOT NULL COMMENT '字典数据ID',
  `type_id` bigint NOT NULL COMMENT '字典类型ID',
  `label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典标签',
  `value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典值',
  `order_num` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0禁用 1启用）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type_id`(`type_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict_data
-- ----------------------------
INSERT INTO `sys_dict_data` VALUES (1, 1, '正常', '1', 1, 1, NULL, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_dict_data` VALUES (2, 1, '禁用', '0', 2, 1, NULL, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_dict_data` VALUES (3, 2, '启用', '1', 1, 1, NULL, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_dict_data` VALUES (4, 2, '停用', '0', 2, 1, NULL, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type`  (
  `id` bigint NOT NULL COMMENT '字典类型ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典名称',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典编码',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0禁用 1启用）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典类型表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict_type
-- ----------------------------
INSERT INTO `sys_dict_type` VALUES (1, '用户状态', 'sys_user_status', 1, '用户状态列表', NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_dict_type` VALUES (2, '通用状态', 'sys_common_status', 1, '通用状态列表', NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);

-- ----------------------------
-- Table structure for sys_login_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log`  (
  `id` bigint NOT NULL COMMENT '日志ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '登录地点',
  `browser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '浏览器',
  `os` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作系统',
  `status` tinyint NULL DEFAULT NULL COMMENT '状态（0失败 1成功）',
  `message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '消息',
  `login_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_username`(`username` ASC) USING BTREE,
  INDEX `idx_login_time`(`login_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '登录日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_login_log
-- ----------------------------

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint NOT NULL COMMENT '菜单ID',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父菜单ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单名称',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '路由地址',
  `component` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '组件路径',
  `permission` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '权限标识',
  `type` tinyint NOT NULL DEFAULT 0 COMMENT '菜单类型（0目录 1菜单 2按钮）',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单图标',
  `order_num` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0禁用 1启用）',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, 0, '系统管理', '/system', '', '', 0, 'mdi:cog-outline', 9, 1, NULL, 1, '2026-03-02 18:12:56', '2026-07-02 17:42:18', 0);
INSERT INTO `sys_menu` VALUES (2, 0, '日志管理', '/log', '', '', 0, 'mdi:text-box-outline', 10, 1, NULL, 1, '2026-03-02 18:12:56', '2026-07-02 17:42:23', 0);
INSERT INTO `sys_menu` VALUES (100, 1, '用户管理', '/system/user', 'system/user/index', 'system:user:list', 1, 'mdi:account-outline', 1, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (101, 100, '用户新增', NULL, NULL, 'system:user:add', 2, NULL, 1, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (102, 100, '用户修改', NULL, NULL, 'system:user:edit', 2, NULL, 2, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (103, 100, '用户删除', NULL, NULL, 'system:user:delete', 2, NULL, 3, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (200, 1, '角色管理', '/system/role', 'system/role/index', 'system:role:list', 1, 'mdi:shield-account-outline', 2, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (201, 200, '角色新增', NULL, NULL, 'system:role:add', 2, NULL, 1, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (202, 200, '角色修改', NULL, NULL, 'system:role:edit', 2, NULL, 2, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (203, 200, '角色删除', NULL, NULL, 'system:role:delete', 2, NULL, 3, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (300, 1, '菜单管理', '/system/menu', 'system/menu/index', 'system:menu:list', 1, 'mdi:menu', 3, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (301, 300, '菜单新增', NULL, NULL, 'system:menu:add', 2, NULL, 1, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (302, 300, '菜单修改', NULL, NULL, 'system:menu:edit', 2, NULL, 2, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (303, 300, '菜单删除', NULL, NULL, 'system:menu:delete', 2, NULL, 3, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (400, 1, '字典管理', '/system/dict', 'system/dict/index', 'system:dict:list', 1, 'mdi:book-outline', 4, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (401, 400, '字典新增', NULL, NULL, 'system:dict:add', 2, NULL, 1, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (402, 400, '字典修改', NULL, NULL, 'system:dict:edit', 2, NULL, 2, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (403, 400, '字典删除', NULL, NULL, 'system:dict:delete', 2, NULL, 3, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (500, 1, '配置管理', '/system/config', 'system/config/index', 'system:config:list', 1, 'mdi:cog', 5, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (501, 500, '配置新增', NULL, NULL, 'system:config:add', 2, NULL, 1, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (502, 500, '配置修改', NULL, NULL, 'system:config:edit', 2, NULL, 2, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (503, 500, '配置删除', NULL, NULL, 'system:config:delete', 2, NULL, 3, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (600, 2, '登录日志', '/log/login', 'log/login/index', 'log:login:list', 1, 'mdi:login', 1, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (700, 2, '操作日志', '/log/operation', 'log/operation/index', 'log:operation:list', 1, 'mdi:file-document-edit-outline', 2, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (1000, 0, '菜品管理', '/dish', '', '', 0, 'mdi:food', 1, 1, NULL, 1, '2026-03-02 18:12:56', '2026-07-02 17:41:41', 0);
INSERT INTO `sys_menu` VALUES (1001, 1000, '分类管理', '/dish/category', 'view.dish_category', 'dish:category:list', 1, 'mdi:tag-outline', 1, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (1002, 1000, '菜品列表', '/dish/list', 'view.dish_list', 'dish:list', 1, 'mdi:food-variant', 2, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (1007, 1, '会员用户管理', '/manage/member-user', 'view.manage_member-user', 'system:user:list', 1, 'mdi:account-group-outline', 8, 1, 1, 1, '2026-07-04 19:03:59', '2026-07-04 19:03:59', 0);
INSERT INTO `sys_menu` VALUES (1010, 0, '桌台管理', '/table', '', '', 0, 'mdi:table-furniture', 2, 1, NULL, 1, '2026-03-02 18:12:56', '2026-07-02 17:41:44', 0);
INSERT INTO `sys_menu` VALUES (1011, 1010, '桌台列表', '/table/manage', 'view.table_manage', 'table:list', 1, 'mdi:table-chair', 1, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (1012, 1010, '区域管理', '/table/area', 'view.table_area', 'table:area:list', 1, 'mdi:floor-plan', 2, 1, 1, 1, '2026-07-02 19:52:31', '2026-07-02 19:52:31', 0);
INSERT INTO `sys_menu` VALUES (1020, 0, '订单中心', '/order', '', '', 0, 'mdi:clipboard-list-outline', 3, 1, NULL, 1, '2026-03-02 18:12:56', '2026-07-02 17:41:48', 0);
INSERT INTO `sys_menu` VALUES (1021, 1020, '订单列表', '/order/list', 'view.order_list', 'order:list', 1, 'mdi:format-list-bulleted', 1, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (1030, 0, '数据报表', '/report', '', '', 0, 'mdi:chart-bar', 4, 1, NULL, 1, '2026-03-02 18:12:56', '2026-07-02 17:41:51', 0);
INSERT INTO `sys_menu` VALUES (1031, 1030, '营业额统计', '/report/revenue', 'view.report_revenue', 'report:revenue', 1, 'mdi:cash-multiple', 1, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (1032, 1030, '菜品排行', '/report/dish-ranking', 'view.report_dish-ranking', 'report:dish-ranking', 1, 'mdi:trophy-outline', 2, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (1033, 1030, '翻台率', '/report/table-turnover', 'view.report_table-turnover', 'report:table-turnover', 1, 'mdi:rotate-3d-variant', 3, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (1040, 0, '设备与日志', '/device', '', '', 0, 'mdi:printer-outline', 5, 1, NULL, 1, '2026-03-02 18:12:56', '2026-07-02 17:41:55', 0);
INSERT INTO `sys_menu` VALUES (1041, 1040, '打印机管理', '/device/printer', 'view.device_printer', 'print:manage', 1, 'mdi:printer', 1, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (1042, 1040, '审计日志', '/device/audit-log', 'view.device_audit-log', 'audit:list', 1, 'mdi:file-document-outline', 2, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (1043, 1010, '下载桌台二维码', NULL, NULL, 'table:qrcode:download', 2, NULL, 2, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (1044, 1010, '生成桌台二维码', NULL, NULL, 'table:qrcode:generate', 2, NULL, 3, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (1045, 1020, '支付管理', '/device/payment', 'view.device_payment', 'payment:list', 1, 'mdi:cash-multiple', 2, 1, 1, 1, '2026-03-02 21:15:41', '2026-03-03 14:13:07', 0);
INSERT INTO `sys_menu` VALUES (1046, 1020, '评价管理', '/device/review', 'view.device_review', 'review:list', 1, 'mdi:star-outline', 3, 1, 1, 1, '2026-03-02 21:15:41', '2026-03-03 14:13:07', 0);
INSERT INTO `sys_menu` VALUES (1060, 1080, '优惠券管理', '/marketing/coupon', 'view.marketing_coupon', NULL, 1, 'mdi:ticket-percent-outline', 1, 1, 1, 1, '2026-06-26 22:56:03', '2026-06-27 00:39:46', 0);
INSERT INTO `sys_menu` VALUES (1070, 1080, '轮播图管理', '/marketing/banner', 'view.marketing_banner', NULL, 1, 'mdi:image-multiple-outline', 2, 1, 1, 1, '2026-06-26 23:20:31', '2026-06-27 00:39:46', 0);
INSERT INTO `sys_menu` VALUES (1071, 1081, '消息管理', '/monitor/message', 'view.monitor_message', NULL, 1, 'mdi:message-processing-outline', 1, 1, 1, 1, '2026-06-27 00:21:05', '2026-06-27 00:39:46', 0);
INSERT INTO `sys_menu` VALUES (1080, 0, '营销中心', '/marketing', '', '', 0, 'mdi:bullhorn-outline', 7, 1, 1, 1, '2026-06-27 00:39:46', '2026-07-02 17:42:05', 0);
INSERT INTO `sys_menu` VALUES (1081, 0, '消息监控', '/monitor', '', '', 0, 'mdi:message-alert-outline', 8, 1, 1, 1, '2026-06-27 00:39:46', '2026-07-02 17:42:09', 0);
INSERT INTO `sys_menu` VALUES (1082, 1080, '会员列表', '/marketing/member', 'view.marketing_member', NULL, 1, 'mdi:account-group-outline', 3, 1, 1, 1, '2026-06-30 12:47:21', '2026-06-30 12:47:21', 0);
INSERT INTO `sys_menu` VALUES (1083, 1080, '会员等级', '/marketing/member-level', 'view.marketing_member-level', NULL, 1, 'mdi:medal-outline', 4, 1, 1, 1, '2026-06-30 12:47:29', '2026-06-30 12:47:29', 0);
INSERT INTO `sys_menu` VALUES (1084, 1080, '积分流水', '/marketing/member-points-record', 'view.marketing_member-points-record', NULL, 1, 'mdi:star-circle-outline', 5, 1, 1, 1, '2026-06-30 12:47:41', '2026-06-30 12:47:41', 0);
INSERT INTO `sys_menu` VALUES (1085, 1080, '成长流水', '/marketing/member-growth-record', 'view.marketing_member-growth-record', NULL, 1, 'mdi:chart-line-variant', 6, 1, 1, 1, '2026-06-30 12:47:59', '2026-06-30 12:47:59', 0);
INSERT INTO `sys_menu` VALUES (1091, 1020, '反馈管理', '/device/feedback', 'view.device_feedback', 'feedback:list', 1, 'mdi:message-reply-text-outline', 4, 1, 1, 1, '2026-06-27 16:27:04', '2026-06-27 16:27:04', 0);
INSERT INTO `sys_menu` VALUES (1092, 1, '主题设置', '/system/theme', 'view.manage_theme', 'system:config:list', 1, 'mdi:palette-swatch-outline', 6, 1, 1, 1, '2026-07-02 17:37:19', '2026-07-02 17:37:19', 0);
INSERT INTO `sys_menu` VALUES (1150, 0, '服务收银', '/service', 'layout.base', '', 0, 'mdi:cash-register', 6, 1, 1, 1, '2026-03-02 18:12:56', '2026-07-02 17:41:58', 0);
INSERT INTO `sys_menu` VALUES (1151, 1150, '桌台看板', '/service/table-board', 'view.service_table-board', NULL, 1, 'mdi:monitor-dashboard', 1, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (1152, 1150, '服务员点单', '/service/place-order', 'view.service_place-order', NULL, 1, 'mdi:food-fork-drink', 2, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (1153, 1150, '订单操作', '/service/order-ops', 'view.service_order-ops', NULL, 1, 'mdi:clipboard-edit-outline', 3, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (1154, 1150, '结账', '/service/checkout', 'view.service_checkout', NULL, 1, 'mdi:credit-card-outline', 4, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (1155, 1150, '后厨', '/service/kitchen', 'view.service_kitchen', 'kitchen:tasks', 1, 'mdi:chef-hat', 5, 1, 1, 1, '2026-03-03 15:00:20', '2026-03-03 15:00:20', 0);
INSERT INTO `sys_menu` VALUES (2010, 1010, '桌台列表', NULL, NULL, 'table:list', 2, NULL, 1, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2011, 1010, '桌台开台', NULL, NULL, 'table:open', 2, NULL, 2, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2012, 1010, '桌台清洁', NULL, NULL, 'table:clean', 2, NULL, 3, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2013, 1010, '桌台换桌', NULL, NULL, 'table:change', 2, NULL, 4, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2014, 1010, '桌台订单', NULL, NULL, 'table:order', 2, NULL, 5, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2015, 1010, '桌台CRUD', NULL, NULL, 'table:manage', 2, NULL, 6, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2016, 1010, '桌台二维码下载', NULL, NULL, 'table:qrcode:download', 2, NULL, 7, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2017, 1010, '桌台二维码生成', NULL, NULL, 'table:qrcode:generate', 2, NULL, 8, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2018, 1010, '区域列表', NULL, NULL, 'table:area:list', 2, NULL, 9, 1, 1, 1, '2026-07-02 19:52:31', '2026-07-02 19:52:31', 0);
INSERT INTO `sys_menu` VALUES (2019, 1010, '区域维护', NULL, NULL, 'table:area:manage', 2, NULL, 10, 1, 1, 1, '2026-07-02 19:52:31', '2026-07-02 19:52:31', 0);
INSERT INTO `sys_menu` VALUES (2020, 1020, '订单查看', NULL, NULL, 'order:view', 2, NULL, 1, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2021, 1020, '订单创建', NULL, NULL, 'order:create', 2, NULL, 2, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2022, 1020, '订单加菜', NULL, NULL, 'order:add-item', 2, NULL, 3, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2023, 1020, '订单催单', NULL, NULL, 'order:rush', 2, NULL, 4, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2024, 1020, '订单打折', NULL, NULL, 'order:discount', 2, NULL, 5, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2025, 1020, '订单赠送', NULL, NULL, 'order:gift', 2, NULL, 6, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2026, 1020, '订单退菜', NULL, NULL, 'order:return', 2, NULL, 7, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2027, 1020, '订单换菜', NULL, NULL, 'order:replace', 2, NULL, 8, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2028, 1020, '订单列表', NULL, NULL, 'order:list', 2, NULL, 9, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2030, 1000, '菜品查看', NULL, NULL, 'dish:view', 2, NULL, 1, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2031, 1000, '菜品管理', NULL, NULL, 'dish:manage', 2, NULL, 2, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2032, 1000, '菜品估清', NULL, NULL, 'dish:sold-out', 2, NULL, 3, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2040, 1150, '现金支付', NULL, NULL, 'payment:cash', 2, NULL, 1, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2041, 1150, '扫码支付', NULL, NULL, 'payment:qrcode', 2, NULL, 2, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2042, 1150, '分单结账', NULL, NULL, 'payment:split-bill', 2, NULL, 3, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2043, 2003, '支付记录列表', NULL, NULL, 'payment:list', 2, NULL, 4, 1, 1, 1, '2026-03-02 20:38:29', '2026-03-02 20:38:29', 0);
INSERT INTO `sys_menu` VALUES (2050, 1150, '后厨任务列表', NULL, NULL, 'kitchen:tasks', 2, NULL, 1, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2051, 1150, '后厨接单', NULL, NULL, 'kitchen:accept', 2, NULL, 2, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2052, 1150, '后厨划单', NULL, NULL, 'kitchen:complete', 2, NULL, 3, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2060, 1030, '营业额统计', NULL, NULL, 'report:revenue', 2, NULL, 1, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2061, 1030, '菜品排行', NULL, NULL, 'report:dish-ranking', 2, NULL, 2, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2062, 1030, '翻台率统计', NULL, NULL, 'report:table-turnover', 2, NULL, 3, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2063, 1030, '报表导出', NULL, NULL, 'report:export', 2, NULL, 4, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2064, 1060, '用户优惠券查看', NULL, NULL, 'coupon:user:list', 2, NULL, 5, 1, 1, 1, '2026-06-26 22:56:04', '2026-06-26 22:56:04', 0);
INSERT INTO `sys_menu` VALUES (2070, 1040, '打印机管理', NULL, NULL, 'print:manage', 2, NULL, 1, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2071, 1040, '重新打印', NULL, NULL, 'print:reprint', 2, NULL, 2, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2080, 1040, '评价查看', NULL, NULL, 'review:view', 2, NULL, 1, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2081, 2007, '评价列表', NULL, NULL, 'review:list', 2, NULL, 2, 1, 1, 1, '2026-03-02 20:38:29', '2026-03-02 20:38:29', 0);
INSERT INTO `sys_menu` VALUES (2090, 1040, '审计日志查询', NULL, NULL, 'audit:list', 2, NULL, 1, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2091, 1040, '审计日志导出', NULL, NULL, 'audit:export', 2, NULL, 2, 1, 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_menu` VALUES (2160, 1060, '模板查看', NULL, NULL, 'coupon:template:list', 2, NULL, 1, 1, 1, 1, '2026-06-26 22:57:49', '2026-06-26 22:57:49', 0);
INSERT INTO `sys_menu` VALUES (2161, 1060, '模板创建', NULL, NULL, 'coupon:template:create', 2, NULL, 2, 1, 1, 1, '2026-06-26 22:57:49', '2026-06-26 22:57:49', 0);
INSERT INTO `sys_menu` VALUES (2162, 1060, '模板更新', NULL, NULL, 'coupon:template:update', 2, NULL, 3, 1, 1, 1, '2026-06-26 22:57:49', '2026-06-26 22:57:49', 0);
INSERT INTO `sys_menu` VALUES (2163, 1060, '发券操作', NULL, NULL, 'coupon:grant', 2, NULL, 4, 1, 1, 1, '2026-06-26 22:57:49', '2026-06-26 22:57:49', 0);
INSERT INTO `sys_menu` VALUES (2164, 1060, '用户优惠券查看', NULL, NULL, 'coupon:user:list', 2, NULL, 5, 1, 1, 1, '2026-06-26 22:57:49', '2026-06-26 22:57:49', 0);
INSERT INTO `sys_menu` VALUES (2170, 1070, '轮播图列表', NULL, NULL, 'banner:list', 2, NULL, 1, 1, 1, 1, '2026-06-26 23:20:31', '2026-06-26 23:20:31', 0);
INSERT INTO `sys_menu` VALUES (2171, 1070, '轮播图创建', NULL, NULL, 'banner:create', 2, NULL, 2, 1, 1, 1, '2026-06-26 23:20:31', '2026-06-26 23:20:31', 0);
INSERT INTO `sys_menu` VALUES (2172, 1070, '轮播图更新', NULL, NULL, 'banner:update', 2, NULL, 3, 1, 1, 1, '2026-06-26 23:20:31', '2026-06-26 23:20:31', 0);
INSERT INTO `sys_menu` VALUES (2173, 1060, '发券任务查看', NULL, NULL, 'coupon:task:list', 2, NULL, 6, 1, 1, 1, '2026-06-27 00:21:05', '2026-06-27 00:21:05', 0);
INSERT INTO `sys_menu` VALUES (2191, 1091, '反馈列表', NULL, NULL, 'feedback:list', 2, NULL, 1, 1, 1, 1, '2026-06-27 16:27:04', '2026-06-27 16:27:04', 0);
INSERT INTO `sys_menu` VALUES (2192, 1091, '反馈回复', NULL, NULL, 'feedback:reply', 2, NULL, 2, 1, 1, 1, '2026-06-27 16:27:04', '2026-06-27 16:27:04', 0);
INSERT INTO `sys_menu` VALUES (2270, 1071, '消息列表', NULL, NULL, 'mq:message:list', 2, NULL, 1, 1, 1, 1, '2026-06-27 00:21:05', '2026-06-27 00:21:05', 0);
INSERT INTO `sys_menu` VALUES (2271, 1071, '消息重试', NULL, NULL, 'mq:message:retry', 2, NULL, 2, 1, 1, 1, '2026-06-27 00:21:05', '2026-06-27 00:21:05', 0);

-- ----------------------------
-- Table structure for sys_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log`  (
  `id` bigint NOT NULL COMMENT '日志ID',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '模块名称',
  `operation` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作描述',
  `method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '方法名称',
  `request_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '请求URL',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '请求方式',
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '请求参数',
  `response_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '响应结果',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `duration` bigint NULL DEFAULT NULL COMMENT '执行时长（毫秒）',
  `status` tinyint NULL DEFAULT NULL COMMENT '状态（0失败 1成功）',
  `error_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '错误信息',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_operation_log
-- ----------------------------

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL COMMENT '角色ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色编码',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0禁用 1启用）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '超级管理员', 'admin', 1, '拥有所有权限', NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_role` VALUES (2, '普通用户', 'user', 1, '默认角色', NULL, NULL, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_role` VALUES (1000, '餐厅管理员', 'RESTAURANT_ADMIN', 1, '餐厅管理员，拥有点餐系统所有权限', 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_role` VALUES (1001, '服务员', 'WAITER', 1, '服务员，负责桌台管理、点单、加菜、催单', 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_role` VALUES (1002, '收银员', 'CASHIER', 1, '收银员，负责结账、支付、订单操作（打折/赠送/退菜/换菜）', 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_role` VALUES (1003, '后厨', 'KITCHEN', 1, '后厨人员，负责接单、划单、估清管理', 1, 1, '2026-03-02 18:12:56', '2026-03-02 18:12:56', 0);
INSERT INTO `sys_role` VALUES (1004, '运营', 'MARKETING_OPERATOR', 1, '负责优惠券、轮播图、会员运营、评价反馈', NULL, NULL, '2026-07-04 13:34:45', '2026-07-04 13:34:45', 0);
INSERT INTO `sys_role` VALUES (1005, '财务', 'FINANCE_AUDITOR', 1, '负责订单、支付、报表、审计只读与导出', NULL, NULL, '2026-07-04 13:34:45', '2026-07-04 13:34:45', 0);

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`, `menu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色菜单关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (1, 1);
INSERT INTO `sys_role_menu` VALUES (1, 2);
INSERT INTO `sys_role_menu` VALUES (1, 100);
INSERT INTO `sys_role_menu` VALUES (1, 101);
INSERT INTO `sys_role_menu` VALUES (1, 102);
INSERT INTO `sys_role_menu` VALUES (1, 103);
INSERT INTO `sys_role_menu` VALUES (1, 200);
INSERT INTO `sys_role_menu` VALUES (1, 201);
INSERT INTO `sys_role_menu` VALUES (1, 202);
INSERT INTO `sys_role_menu` VALUES (1, 203);
INSERT INTO `sys_role_menu` VALUES (1, 300);
INSERT INTO `sys_role_menu` VALUES (1, 301);
INSERT INTO `sys_role_menu` VALUES (1, 302);
INSERT INTO `sys_role_menu` VALUES (1, 303);
INSERT INTO `sys_role_menu` VALUES (1, 400);
INSERT INTO `sys_role_menu` VALUES (1, 401);
INSERT INTO `sys_role_menu` VALUES (1, 402);
INSERT INTO `sys_role_menu` VALUES (1, 403);
INSERT INTO `sys_role_menu` VALUES (1, 500);
INSERT INTO `sys_role_menu` VALUES (1, 501);
INSERT INTO `sys_role_menu` VALUES (1, 502);
INSERT INTO `sys_role_menu` VALUES (1, 503);
INSERT INTO `sys_role_menu` VALUES (1, 600);
INSERT INTO `sys_role_menu` VALUES (1, 700);
INSERT INTO `sys_role_menu` VALUES (1, 1000);
INSERT INTO `sys_role_menu` VALUES (1, 1001);
INSERT INTO `sys_role_menu` VALUES (1, 1002);
INSERT INTO `sys_role_menu` VALUES (1, 1007);
INSERT INTO `sys_role_menu` VALUES (1, 1010);
INSERT INTO `sys_role_menu` VALUES (1, 1011);
INSERT INTO `sys_role_menu` VALUES (1, 1012);
INSERT INTO `sys_role_menu` VALUES (1, 1020);
INSERT INTO `sys_role_menu` VALUES (1, 1021);
INSERT INTO `sys_role_menu` VALUES (1, 1030);
INSERT INTO `sys_role_menu` VALUES (1, 1031);
INSERT INTO `sys_role_menu` VALUES (1, 1032);
INSERT INTO `sys_role_menu` VALUES (1, 1033);
INSERT INTO `sys_role_menu` VALUES (1, 1040);
INSERT INTO `sys_role_menu` VALUES (1, 1041);
INSERT INTO `sys_role_menu` VALUES (1, 1042);
INSERT INTO `sys_role_menu` VALUES (1, 1043);
INSERT INTO `sys_role_menu` VALUES (1, 1044);
INSERT INTO `sys_role_menu` VALUES (1, 1045);
INSERT INTO `sys_role_menu` VALUES (1, 1046);
INSERT INTO `sys_role_menu` VALUES (1, 1060);
INSERT INTO `sys_role_menu` VALUES (1, 1070);
INSERT INTO `sys_role_menu` VALUES (1, 1071);
INSERT INTO `sys_role_menu` VALUES (1, 1080);
INSERT INTO `sys_role_menu` VALUES (1, 1081);
INSERT INTO `sys_role_menu` VALUES (1, 1082);
INSERT INTO `sys_role_menu` VALUES (1, 1083);
INSERT INTO `sys_role_menu` VALUES (1, 1084);
INSERT INTO `sys_role_menu` VALUES (1, 1085);
INSERT INTO `sys_role_menu` VALUES (1, 1091);
INSERT INTO `sys_role_menu` VALUES (1, 1092);
INSERT INTO `sys_role_menu` VALUES (1, 1150);
INSERT INTO `sys_role_menu` VALUES (1, 1151);
INSERT INTO `sys_role_menu` VALUES (1, 1152);
INSERT INTO `sys_role_menu` VALUES (1, 1153);
INSERT INTO `sys_role_menu` VALUES (1, 1154);
INSERT INTO `sys_role_menu` VALUES (1, 1155);
INSERT INTO `sys_role_menu` VALUES (1, 2018);
INSERT INTO `sys_role_menu` VALUES (1, 2019);
INSERT INTO `sys_role_menu` VALUES (1, 2043);
INSERT INTO `sys_role_menu` VALUES (1, 2060);
INSERT INTO `sys_role_menu` VALUES (1, 2061);
INSERT INTO `sys_role_menu` VALUES (1, 2062);
INSERT INTO `sys_role_menu` VALUES (1, 2063);
INSERT INTO `sys_role_menu` VALUES (1, 2064);
INSERT INTO `sys_role_menu` VALUES (1, 2081);
INSERT INTO `sys_role_menu` VALUES (1, 2160);
INSERT INTO `sys_role_menu` VALUES (1, 2161);
INSERT INTO `sys_role_menu` VALUES (1, 2162);
INSERT INTO `sys_role_menu` VALUES (1, 2163);
INSERT INTO `sys_role_menu` VALUES (1, 2164);
INSERT INTO `sys_role_menu` VALUES (1, 2170);
INSERT INTO `sys_role_menu` VALUES (1, 2171);
INSERT INTO `sys_role_menu` VALUES (1, 2172);
INSERT INTO `sys_role_menu` VALUES (1, 2173);
INSERT INTO `sys_role_menu` VALUES (1, 2191);
INSERT INTO `sys_role_menu` VALUES (1, 2192);
INSERT INTO `sys_role_menu` VALUES (1, 2270);
INSERT INTO `sys_role_menu` VALUES (1, 2271);
INSERT INTO `sys_role_menu` VALUES (1000, 1012);
INSERT INTO `sys_role_menu` VALUES (1000, 1045);
INSERT INTO `sys_role_menu` VALUES (1000, 1046);
INSERT INTO `sys_role_menu` VALUES (1000, 1060);
INSERT INTO `sys_role_menu` VALUES (1000, 1070);
INSERT INTO `sys_role_menu` VALUES (1000, 1071);
INSERT INTO `sys_role_menu` VALUES (1000, 1080);
INSERT INTO `sys_role_menu` VALUES (1000, 1081);
INSERT INTO `sys_role_menu` VALUES (1000, 1082);
INSERT INTO `sys_role_menu` VALUES (1000, 1083);
INSERT INTO `sys_role_menu` VALUES (1000, 1084);
INSERT INTO `sys_role_menu` VALUES (1000, 1085);
INSERT INTO `sys_role_menu` VALUES (1000, 1091);
INSERT INTO `sys_role_menu` VALUES (1000, 1092);
INSERT INTO `sys_role_menu` VALUES (1000, 1150);
INSERT INTO `sys_role_menu` VALUES (1000, 1151);
INSERT INTO `sys_role_menu` VALUES (1000, 1152);
INSERT INTO `sys_role_menu` VALUES (1000, 1153);
INSERT INTO `sys_role_menu` VALUES (1000, 1154);
INSERT INTO `sys_role_menu` VALUES (1000, 1155);
INSERT INTO `sys_role_menu` VALUES (1000, 2010);
INSERT INTO `sys_role_menu` VALUES (1000, 2011);
INSERT INTO `sys_role_menu` VALUES (1000, 2012);
INSERT INTO `sys_role_menu` VALUES (1000, 2013);
INSERT INTO `sys_role_menu` VALUES (1000, 2014);
INSERT INTO `sys_role_menu` VALUES (1000, 2015);
INSERT INTO `sys_role_menu` VALUES (1000, 2016);
INSERT INTO `sys_role_menu` VALUES (1000, 2017);
INSERT INTO `sys_role_menu` VALUES (1000, 2018);
INSERT INTO `sys_role_menu` VALUES (1000, 2019);
INSERT INTO `sys_role_menu` VALUES (1000, 2020);
INSERT INTO `sys_role_menu` VALUES (1000, 2021);
INSERT INTO `sys_role_menu` VALUES (1000, 2022);
INSERT INTO `sys_role_menu` VALUES (1000, 2023);
INSERT INTO `sys_role_menu` VALUES (1000, 2024);
INSERT INTO `sys_role_menu` VALUES (1000, 2025);
INSERT INTO `sys_role_menu` VALUES (1000, 2026);
INSERT INTO `sys_role_menu` VALUES (1000, 2027);
INSERT INTO `sys_role_menu` VALUES (1000, 2028);
INSERT INTO `sys_role_menu` VALUES (1000, 2030);
INSERT INTO `sys_role_menu` VALUES (1000, 2031);
INSERT INTO `sys_role_menu` VALUES (1000, 2032);
INSERT INTO `sys_role_menu` VALUES (1000, 2040);
INSERT INTO `sys_role_menu` VALUES (1000, 2041);
INSERT INTO `sys_role_menu` VALUES (1000, 2042);
INSERT INTO `sys_role_menu` VALUES (1000, 2043);
INSERT INTO `sys_role_menu` VALUES (1000, 2050);
INSERT INTO `sys_role_menu` VALUES (1000, 2051);
INSERT INTO `sys_role_menu` VALUES (1000, 2052);
INSERT INTO `sys_role_menu` VALUES (1000, 2060);
INSERT INTO `sys_role_menu` VALUES (1000, 2061);
INSERT INTO `sys_role_menu` VALUES (1000, 2062);
INSERT INTO `sys_role_menu` VALUES (1000, 2063);
INSERT INTO `sys_role_menu` VALUES (1000, 2064);
INSERT INTO `sys_role_menu` VALUES (1000, 2070);
INSERT INTO `sys_role_menu` VALUES (1000, 2071);
INSERT INTO `sys_role_menu` VALUES (1000, 2080);
INSERT INTO `sys_role_menu` VALUES (1000, 2081);
INSERT INTO `sys_role_menu` VALUES (1000, 2090);
INSERT INTO `sys_role_menu` VALUES (1000, 2091);
INSERT INTO `sys_role_menu` VALUES (1000, 2160);
INSERT INTO `sys_role_menu` VALUES (1000, 2161);
INSERT INTO `sys_role_menu` VALUES (1000, 2162);
INSERT INTO `sys_role_menu` VALUES (1000, 2163);
INSERT INTO `sys_role_menu` VALUES (1000, 2164);
INSERT INTO `sys_role_menu` VALUES (1000, 2170);
INSERT INTO `sys_role_menu` VALUES (1000, 2171);
INSERT INTO `sys_role_menu` VALUES (1000, 2172);
INSERT INTO `sys_role_menu` VALUES (1000, 2173);
INSERT INTO `sys_role_menu` VALUES (1000, 2191);
INSERT INTO `sys_role_menu` VALUES (1000, 2192);
INSERT INTO `sys_role_menu` VALUES (1000, 2270);
INSERT INTO `sys_role_menu` VALUES (1000, 2271);
INSERT INTO `sys_role_menu` VALUES (1001, 1150);
INSERT INTO `sys_role_menu` VALUES (1001, 1151);
INSERT INTO `sys_role_menu` VALUES (1001, 1152);
INSERT INTO `sys_role_menu` VALUES (1001, 1153);
INSERT INTO `sys_role_menu` VALUES (1001, 2010);
INSERT INTO `sys_role_menu` VALUES (1001, 2011);
INSERT INTO `sys_role_menu` VALUES (1001, 2014);
INSERT INTO `sys_role_menu` VALUES (1001, 2020);
INSERT INTO `sys_role_menu` VALUES (1001, 2021);
INSERT INTO `sys_role_menu` VALUES (1001, 2022);
INSERT INTO `sys_role_menu` VALUES (1001, 2023);
INSERT INTO `sys_role_menu` VALUES (1001, 2030);
INSERT INTO `sys_role_menu` VALUES (1001, 2071);
INSERT INTO `sys_role_menu` VALUES (1002, 1150);
INSERT INTO `sys_role_menu` VALUES (1002, 1151);
INSERT INTO `sys_role_menu` VALUES (1002, 1153);
INSERT INTO `sys_role_menu` VALUES (1002, 1154);
INSERT INTO `sys_role_menu` VALUES (1002, 2010);
INSERT INTO `sys_role_menu` VALUES (1002, 2011);
INSERT INTO `sys_role_menu` VALUES (1002, 2014);
INSERT INTO `sys_role_menu` VALUES (1002, 2020);
INSERT INTO `sys_role_menu` VALUES (1002, 2024);
INSERT INTO `sys_role_menu` VALUES (1002, 2025);
INSERT INTO `sys_role_menu` VALUES (1002, 2026);
INSERT INTO `sys_role_menu` VALUES (1002, 2027);
INSERT INTO `sys_role_menu` VALUES (1002, 2028);
INSERT INTO `sys_role_menu` VALUES (1002, 2030);
INSERT INTO `sys_role_menu` VALUES (1002, 2040);
INSERT INTO `sys_role_menu` VALUES (1002, 2041);
INSERT INTO `sys_role_menu` VALUES (1002, 2042);
INSERT INTO `sys_role_menu` VALUES (1002, 2071);
INSERT INTO `sys_role_menu` VALUES (1003, 1150);
INSERT INTO `sys_role_menu` VALUES (1003, 1155);
INSERT INTO `sys_role_menu` VALUES (1003, 2030);
INSERT INTO `sys_role_menu` VALUES (1003, 2032);
INSERT INTO `sys_role_menu` VALUES (1003, 2050);
INSERT INTO `sys_role_menu` VALUES (1003, 2051);
INSERT INTO `sys_role_menu` VALUES (1003, 2052);
INSERT INTO `sys_role_menu` VALUES (1004, 1030);
INSERT INTO `sys_role_menu` VALUES (1004, 1031);
INSERT INTO `sys_role_menu` VALUES (1004, 1032);
INSERT INTO `sys_role_menu` VALUES (1004, 1033);
INSERT INTO `sys_role_menu` VALUES (1004, 1046);
INSERT INTO `sys_role_menu` VALUES (1004, 1060);
INSERT INTO `sys_role_menu` VALUES (1004, 1070);
INSERT INTO `sys_role_menu` VALUES (1004, 1080);
INSERT INTO `sys_role_menu` VALUES (1004, 1082);
INSERT INTO `sys_role_menu` VALUES (1004, 1083);
INSERT INTO `sys_role_menu` VALUES (1004, 1084);
INSERT INTO `sys_role_menu` VALUES (1004, 1085);
INSERT INTO `sys_role_menu` VALUES (1004, 1091);
INSERT INTO `sys_role_menu` VALUES (1004, 2060);
INSERT INTO `sys_role_menu` VALUES (1004, 2061);
INSERT INTO `sys_role_menu` VALUES (1004, 2062);
INSERT INTO `sys_role_menu` VALUES (1004, 2080);
INSERT INTO `sys_role_menu` VALUES (1004, 2081);
INSERT INTO `sys_role_menu` VALUES (1004, 2160);
INSERT INTO `sys_role_menu` VALUES (1004, 2161);
INSERT INTO `sys_role_menu` VALUES (1004, 2162);
INSERT INTO `sys_role_menu` VALUES (1004, 2163);
INSERT INTO `sys_role_menu` VALUES (1004, 2164);
INSERT INTO `sys_role_menu` VALUES (1004, 2170);
INSERT INTO `sys_role_menu` VALUES (1004, 2171);
INSERT INTO `sys_role_menu` VALUES (1004, 2172);
INSERT INTO `sys_role_menu` VALUES (1004, 2173);
INSERT INTO `sys_role_menu` VALUES (1004, 2191);
INSERT INTO `sys_role_menu` VALUES (1004, 2192);
INSERT INTO `sys_role_menu` VALUES (1005, 1020);
INSERT INTO `sys_role_menu` VALUES (1005, 1030);
INSERT INTO `sys_role_menu` VALUES (1005, 1031);
INSERT INTO `sys_role_menu` VALUES (1005, 1032);
INSERT INTO `sys_role_menu` VALUES (1005, 1033);
INSERT INTO `sys_role_menu` VALUES (1005, 1040);
INSERT INTO `sys_role_menu` VALUES (1005, 1042);
INSERT INTO `sys_role_menu` VALUES (1005, 1045);
INSERT INTO `sys_role_menu` VALUES (1005, 1046);
INSERT INTO `sys_role_menu` VALUES (1005, 1150);
INSERT INTO `sys_role_menu` VALUES (1005, 1151);
INSERT INTO `sys_role_menu` VALUES (1005, 1153);
INSERT INTO `sys_role_menu` VALUES (1005, 1154);
INSERT INTO `sys_role_menu` VALUES (1005, 2020);
INSERT INTO `sys_role_menu` VALUES (1005, 2028);
INSERT INTO `sys_role_menu` VALUES (1005, 2040);
INSERT INTO `sys_role_menu` VALUES (1005, 2041);
INSERT INTO `sys_role_menu` VALUES (1005, 2042);
INSERT INTO `sys_role_menu` VALUES (1005, 2043);
INSERT INTO `sys_role_menu` VALUES (1005, 2060);
INSERT INTO `sys_role_menu` VALUES (1005, 2061);
INSERT INTO `sys_role_menu` VALUES (1005, 2062);
INSERT INTO `sys_role_menu` VALUES (1005, 2063);
INSERT INTO `sys_role_menu` VALUES (1005, 2071);
INSERT INTO `sys_role_menu` VALUES (1005, 2080);
INSERT INTO `sys_role_menu` VALUES (1005, 2081);
INSERT INTO `sys_role_menu` VALUES (1005, 2090);
INSERT INTO `sys_role_menu` VALUES (1005, 2091);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `openid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '微信openid',
  `user_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'APP' COMMENT '用户类型：BACKEND/APP/STRESS',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0禁用 1启用）',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$D273KbUAFVgQ3IUx4LD8m.r9bowenLvwNgKXc4.e2MZxzoTK48T4W', '超级管理员', NULL, NULL, NULL, 'BACKEND', NULL, 1, NULL, NULL, '2026-03-02 18:12:56', '2026-07-04 13:45:15', 0);
INSERT INTO `sys_user` VALUES (2, 'waiter01', '$2a$10$D273KbUAFVgQ3IUx4LD8m.r9bowenLvwNgKXc4.e2MZxzoTK48T4W', '服务员小王', NULL, NULL, NULL, 'BACKEND', NULL, 1, NULL, NULL, '2026-03-02 18:12:57', '2026-07-04 13:45:15', 0);
INSERT INTO `sys_user` VALUES (3, 'waiter02', '$2a$10$D273KbUAFVgQ3IUx4LD8m.r9bowenLvwNgKXc4.e2MZxzoTK48T4W', '服务员小李', NULL, NULL, NULL, 'BACKEND', NULL, 1, NULL, NULL, '2026-03-02 18:12:57', '2026-07-04 13:45:15', 0);
INSERT INTO `sys_user` VALUES (4, 'cashier01', '$2a$10$D273KbUAFVgQ3IUx4LD8m.r9bowenLvwNgKXc4.e2MZxzoTK48T4W', '收银员小张', NULL, NULL, NULL, 'BACKEND', NULL, 1, NULL, NULL, '2026-03-02 18:12:57', '2026-07-04 13:45:15', 0);
INSERT INTO `sys_user` VALUES (5, 'chef01', '$2a$10$D273KbUAFVgQ3IUx4LD8m.r9bowenLvwNgKXc4.e2MZxzoTK48T4W', '厨师老陈', NULL, NULL, NULL, 'BACKEND', NULL, 1, NULL, NULL, '2026-03-02 18:12:57', '2026-07-04 13:45:15', 0);
INSERT INTO `sys_user` VALUES (6, 'chef02', '$2a$10$D273KbUAFVgQ3IUx4LD8m.r9bowenLvwNgKXc4.e2MZxzoTK48T4W', '厨师老刘', NULL, NULL, NULL, 'BACKEND', NULL, 1, NULL, NULL, '2026-03-02 18:12:57', '2026-07-04 13:45:15', 0);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1);
INSERT INTO `sys_user_role` VALUES (2, 1001);
INSERT INTO `sys_user_role` VALUES (3, 1001);
INSERT INTO `sys_user_role` VALUES (4, 1002);
INSERT INTO `sys_user_role` VALUES (5, 1003);
INSERT INTO `sys_user_role` VALUES (6, 1003);

-- ----------------------------
-- Table structure for table_area
-- ----------------------------
DROP TABLE IF EXISTS `table_area`;
CREATE TABLE `table_area`  (
  `id` bigint NOT NULL COMMENT '区域ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '区域名称',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0禁用 1启用）',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '桌台区域表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of table_area
-- ----------------------------
INSERT INTO `table_area` VALUES (31001, '包间', 0, 1, '', 1, 1, '2026-07-02 19:52:03', '2026-07-02 20:31:39', 0);
INSERT INTO `table_area` VALUES (31002, '大厅', 10, 1, '', 1, 1, '2026-07-02 19:52:03', '2026-07-02 20:31:43', 0);
INSERT INTO `table_area` VALUES (31003, '露台', 20, 1, '', 1, 1, '2026-07-02 19:52:03', '2026-07-02 20:31:47', 0);

-- ----------------------------
-- Table structure for user_coupon
-- ----------------------------
DROP TABLE IF EXISTS `user_coupon`;
CREATE TABLE `user_coupon`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户名快照',
  `nickname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '昵称快照',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号快照',
  `coupon_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '券名称快照',
  `coupon_type` tinyint NOT NULL COMMENT '优惠券类型（1满减 2折扣）',
  `threshold_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛金额',
  `discount_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  `discount_rate` decimal(4, 2) NULL DEFAULT NULL COMMENT '折扣比例',
  `source_type` tinyint NOT NULL DEFAULT 1 COMMENT '来源类型（1后台发放 2全员发放）',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0未使用 1已使用 2已过期 3已锁定）',
  `received_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `valid_from` datetime NOT NULL COMMENT '生效时间',
  `valid_to` datetime NOT NULL COMMENT '失效时间',
  `used_time` datetime NULL DEFAULT NULL COMMENT '使用时间',
  `order_id` bigint NULL DEFAULT NULL COMMENT '使用订单ID',
  `grant_task_id` bigint NULL DEFAULT NULL COMMENT '发券任务ID',
  `available_weekdays` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '可用星期，1-7 表示周一到周日，逗号分隔，NULL 表示每天可用',
  `create_by` bigint NULL DEFAULT NULL,
  `update_by` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_coupon_user_status`(`user_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_user_coupon_template_user`(`template_id` ASC, `user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户优惠券表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_coupon
-- ----------------------------

-- ----------------------------
-- Table structure for user_feedback
-- ----------------------------
DROP TABLE IF EXISTS `user_feedback`;
CREATE TABLE `user_feedback`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `customer_openid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '反馈用户openid',
  `customer_phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户手机号快照',
  `contact_phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系手机号',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '反馈内容',
  `reply_content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '回复内容',
  `reply_time` datetime NULL DEFAULT NULL COMMENT '回复时间',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0待回复 1已回复',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_feedback_openid`(`customer_openid` ASC) USING BTREE,
  INDEX `idx_user_feedback_status_create_time`(`status` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户反馈表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_feedback
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
