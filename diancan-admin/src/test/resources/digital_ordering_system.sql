-- =============================================
-- 数字化点餐系统数据库表结构
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 菜品分类表
-- ----------------------------
DROP TABLE IF EXISTS `dish_category`;
CREATE TABLE `dish_category` (
    `id` bigint NOT NULL COMMENT '分类ID',
    `name` varchar(50) NOT NULL COMMENT '分类名称',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序序号',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0停用 1启用）',
    `image` varchar(500) DEFAULT NULL COMMENT '分类图片',
    `spec_template` tinyint NOT NULL DEFAULT 1 COMMENT '规格模板（0无规格 1辣度 2饮品规格）',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品分类表';

-- ----------------------------
-- 菜品分类默认规格关联表
-- ----------------------------
DROP TABLE IF EXISTS `dish_category_spec`;
CREATE TABLE `dish_category_spec` (
    `id` bigint NOT NULL COMMENT '关联ID',
    `category_id` bigint NOT NULL COMMENT '分类ID',
    `spec_group_id` bigint NOT NULL COMMENT '规格组ID',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dish_category_spec` (`category_id`, `spec_group_id`),
    KEY `idx_dish_category_spec_group_id` (`spec_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='分类默认规格关联表';

-- ----------------------------
-- 菜品表
-- ----------------------------
DROP TABLE IF EXISTS `dish`;
CREATE TABLE `dish` (
    `id` bigint NOT NULL COMMENT '菜品ID',
    `category_id` bigint NOT NULL COMMENT '所属分类ID',
    `name` varchar(100) NOT NULL COMMENT '菜品名称',
    `price` decimal(10,2) NOT NULL COMMENT '价格',
    `image` varchar(500) DEFAULT NULL COMMENT '图片URL',
    `thumbnail` varchar(500) DEFAULT NULL COMMENT '缩略图URL',
    `spice_level` tinyint NOT NULL DEFAULT 0 COMMENT '辣度标记（0不辣 1微辣 2中辣 3重辣）',
    `spec_values` varchar(1000) DEFAULT NULL COMMENT '扩展规格值（JSON）',
    `ingredients` varchar(500) DEFAULT NULL COMMENT '配料列表（JSON数组）',
    `description` varchar(500) DEFAULT NULL COMMENT '简介',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0下架 1上架）',
    `sold_out` tinyint NOT NULL DEFAULT 0 COMMENT '是否售罄（0否 1是）',
    `stock` int NOT NULL DEFAULT -1 COMMENT '库存数量（-1表示不限库存）',
    `preparation_time` int DEFAULT NULL COMMENT '预设制作时限（分钟）',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品表';

-- ----------------------------
-- 菜品规格映射表
-- ----------------------------
DROP TABLE IF EXISTS `dish_spec_mapping`;
CREATE TABLE `dish_spec_mapping` (
    `id` bigint NOT NULL COMMENT '映射ID',
    `dish_id` bigint NOT NULL COMMENT '菜品ID',
    `spec_group_id` bigint NOT NULL COMMENT '规格组ID',
    `option_ids` varchar(500) NOT NULL COMMENT '规格值ID列表，逗号分隔',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_dish_spec_mapping_dish_id` (`dish_id`),
    KEY `idx_dish_spec_mapping_group_id` (`spec_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品规格映射表';

-- ----------------------------
-- 桌台区域表
-- ----------------------------
DROP TABLE IF EXISTS `table_area`;
CREATE TABLE `table_area` (
    `id` bigint NOT NULL COMMENT '区域ID',
    `name` varchar(50) NOT NULL COMMENT '区域名称',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0禁用 1启用）',
    `remark` varchar(200) DEFAULT NULL COMMENT '备注',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='桌台区域表';

-- ----------------------------
-- 桌台表
-- ----------------------------
DROP TABLE IF EXISTS `dining_table`;
CREATE TABLE `dining_table` (
    `id` bigint NOT NULL COMMENT '桌台ID',
    `code` varchar(50) NOT NULL COMMENT '桌台编号（二维码关联）',
    `name` varchar(50) NOT NULL COMMENT '桌台名称（如A1桌）',
    `capacity` int NOT NULL DEFAULT 4 COMMENT '座位数',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0空闲 1占用 2已结账 3待清洁）',
    `qr_code_url` varchar(500) DEFAULT NULL COMMENT '二维码图片URL',
    `area_id` bigint DEFAULT NULL COMMENT '区域ID',
    `area_name` varchar(50) DEFAULT NULL COMMENT '区域名称（如大厅、包间）',
    `current_session_code` varchar(64) DEFAULT NULL COMMENT '当前桌次编码',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_status` (`status`),
    KEY `idx_area_id` (`area_id`),
    KEY `idx_current_session_code` (`current_session_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='桌台表';

-- ----------------------------
-- 订单表（order 为 MySQL 保留字，使用反引号）
-- ----------------------------
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
    `id` bigint NOT NULL COMMENT '订单ID',
    `order_no` varchar(32) NOT NULL COMMENT '订单编号',
    `table_id` bigint DEFAULT NULL COMMENT '关联桌台ID',
    `table_code` varchar(50) DEFAULT NULL COMMENT '桌台编号（冗余）',
    `table_session_code` varchar(64) DEFAULT NULL COMMENT '桌次编码（冗余）',
    `original_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '原始总金额',
    `discount_rate` decimal(3,2) NOT NULL DEFAULT 1.00 COMMENT '折扣比例（默认1.00）',
    `coupon_id` bigint DEFAULT NULL COMMENT '优惠券ID',
    `coupon_name` varchar(100) DEFAULT NULL COMMENT '优惠券名称',
    `coupon_type` tinyint DEFAULT NULL COMMENT '优惠券类型（1满减 2折扣）',
    `coupon_threshold_amount` decimal(10,2) DEFAULT NULL COMMENT '优惠券门槛金额',
    `coupon_discount_amount` decimal(10,2) DEFAULT NULL COMMENT '优惠券减免金额',
    `coupon_discount_rate` decimal(4,2) DEFAULT NULL COMMENT '优惠券折扣比例',
    `actual_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '实付总金额',
    `points_used` int NOT NULL DEFAULT 0 COMMENT '使用积分',
    `points_discount_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '积分抵现金额',
    `paid_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '已支付金额（AA场景）',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0待支付 1已支付 2已取消）',
    `payment_mode` tinyint NOT NULL DEFAULT 1 COMMENT '支付模式（0餐前付 1餐后付）',
    `order_type` tinyint NOT NULL DEFAULT 0 COMMENT '订单类型（0堂食 1外卖）',
    `remark` varchar(500) DEFAULT NULL COMMENT '订单备注',
    `customer_openid` varchar(100) DEFAULT NULL COMMENT '顾客微信openid',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_table_id` (`table_id`),
    KEY `idx_table_session_status` (`table_id`, `table_session_code`, `status`),
    KEY `idx_status` (`status`),
    KEY `idx_customer_openid` (`customer_openid`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单表';

-- ----------------------------
-- 订单项表
-- ----------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
    `id` bigint NOT NULL COMMENT '订单项ID',
    `order_id` bigint NOT NULL COMMENT '所属订单ID',
    `dish_id` bigint NOT NULL COMMENT '菜品ID',
    `dish_name` varchar(100) NOT NULL COMMENT '菜品名称（冗余）',
    `dish_image` varchar(500) DEFAULT NULL COMMENT '菜品图片（冗余）',
    `price` decimal(10,2) NOT NULL COMMENT '下单时单价（冗余）',
    `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
    `amount` decimal(10,2) NOT NULL COMMENT '小计金额',
    `remark` varchar(200) DEFAULT NULL COMMENT '口味备注',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0待制作 1制作中 2已完成）',
    `payment_status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态（0未支付 2已支付）',
    `is_gift` tinyint NOT NULL DEFAULT 0 COMMENT '是否赠送（0否 1是）',
    `added_at` datetime DEFAULT NULL COMMENT '加入订单时间（用于区分加菜）',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_dish_id` (`dish_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单项表';

-- ----------------------------
-- 支付记录表
-- ----------------------------
DROP TABLE IF EXISTS `payment_record`;
CREATE TABLE `payment_record` (
    `id` bigint NOT NULL COMMENT '支付记录ID',
    `order_id` bigint NOT NULL COMMENT '关联订单ID',
    `payment_no` varchar(64) NOT NULL COMMENT '支付流水号',
    `third_party_no` varchar(64) DEFAULT NULL COMMENT '第三方支付流水号',
    `payment_method` tinyint NOT NULL COMMENT '支付方式（0微信 1支付宝 2现金 3会员卡）',
    `amount` decimal(10,2) NOT NULL COMMENT '支付金额',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0待支付 1已支付 2待确认 3已退款）',
    `payer_openid` varchar(100) DEFAULT NULL COMMENT '支付人openid（AA场景）',
    `callback_data` text DEFAULT NULL COMMENT '支付回调原始数据',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_no` (`payment_no`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='支付记录表';

-- ----------------------------
-- 订单评价表
-- ----------------------------
DROP TABLE IF EXISTS `order_review`;
CREATE TABLE `order_review` (
    `id` bigint NOT NULL COMMENT '评价ID',
    `order_id` bigint NOT NULL COMMENT '关联订单ID',
    `overall_rating` tinyint NOT NULL COMMENT '总体评分（1-5）',
    `content` varchar(500) DEFAULT NULL COMMENT '文字评价',
    `customer_openid` varchar(100) DEFAULT NULL COMMENT '评价人openid',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单评价表';

-- ----------------------------
-- 订单项评价表
-- ----------------------------
DROP TABLE IF EXISTS `order_item_review`;
CREATE TABLE `order_item_review` (
    `id` bigint NOT NULL COMMENT '订单项评价ID',
    `review_id` bigint NOT NULL COMMENT '关联评价ID',
    `order_item_id` bigint NOT NULL COMMENT '关联订单项ID',
    `rating` tinyint NOT NULL COMMENT '评分（1-5）',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_review_id` (`review_id`),
    KEY `idx_order_item_id` (`order_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单项评价表';

-- ----------------------------
-- 打印机表
-- ----------------------------
DROP TABLE IF EXISTS `printer`;
CREATE TABLE `printer` (
    `id` bigint NOT NULL COMMENT '打印机ID',
    `name` varchar(50) NOT NULL COMMENT '打印机名称',
    `sn` varchar(100) NOT NULL COMMENT '打印机序列号',
    `type` tinyint NOT NULL DEFAULT 0 COMMENT '类型（0前台 1后厨）',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0离线 1在线）',
    `location` varchar(100) DEFAULT NULL COMMENT '位置描述',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='打印机表';

-- ----------------------------
-- 打印机-分类映射表
-- ----------------------------
DROP TABLE IF EXISTS `printer_category_mapping`;
CREATE TABLE `printer_category_mapping` (
    `id` bigint NOT NULL COMMENT '映射ID',
    `printer_id` bigint NOT NULL COMMENT '打印机ID',
    `category_id` bigint NOT NULL COMMENT '菜品分类ID',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_printer_category` (`printer_id`, `category_id`),
    KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='打印机-分类映射表';

-- ----------------------------
-- 系统配置表
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
    `id` bigint NOT NULL COMMENT '配置ID',
    `name` varchar(100) NOT NULL COMMENT '配置名称',
    `config_key` varchar(100) NOT NULL COMMENT '配置键',
    `config_value` varchar(500) DEFAULT NULL COMMENT '配置值',
    `remark` varchar(255) DEFAULT NULL COMMENT '备注',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统配置表';

-- ----------------------------
-- 订单操作日志表
-- ----------------------------
DROP TABLE IF EXISTS `order_operation_log`;
CREATE TABLE `order_operation_log` (
    `id` bigint NOT NULL COMMENT '日志ID',
    `order_id` bigint NOT NULL COMMENT '关联订单ID',
    `order_item_id` bigint DEFAULT NULL COMMENT '关联订单项ID',
    `operation_type` varchar(20) NOT NULL COMMENT '操作类型（RETURN/REPLACE/GIFT/DISCOUNT/RUSH）',
    `operator_id` bigint NOT NULL COMMENT '操作人ID',
    `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名',
    `reason` varchar(200) DEFAULT NULL COMMENT '操作原因',
    `detail` text DEFAULT NULL COMMENT '操作详情（JSON）',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志（0未删除 1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_item_id` (`order_item_id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单操作日志表';

SET FOREIGN_KEY_CHECKS = 1;
