SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS minipay_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS minipay_product DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS minipay_cart DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS minipay_inventory DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS minipay_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS minipay_payment DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS minipay_promotion DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS minipay_logistics DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS minipay_merchant DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE minipay_user;

CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `password` varchar(128) NOT NULL,
  `nickname` varchar(64) DEFAULT NULL,
  `phone` varchar(32) DEFAULT NULL,
  `role` varchar(32) NOT NULL DEFAULT 'BUYER',
  `status` varchar(32) NOT NULL DEFAULT 'NORMAL',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username_role` (`username`, `role`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `user_address` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `receiver_name` varchar(64) NOT NULL,
  `receiver_phone` varchar(32) NOT NULL,
  `province` varchar(64) DEFAULT NULL,
  `city` varchar(64) DEFAULT NULL,
  `district` varchar(64) DEFAULT NULL,
  `detail_address` varchar(255) NOT NULL,
  `is_default` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `user_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `balance` decimal(10,2) NOT NULL DEFAULT 0.00,
  `status` varchar(32) NOT NULL DEFAULT 'NORMAL',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO `user` (`id`, `username`, `password`, `nickname`, `phone`, `role`, `status`) VALUES
(1, 'buyer', '123456', '林同学', '13000000000', 'BUYER', 'ACTIVE'),
(2, 'merchant', '123456', '商家账号', '13000000001', 'MERCHANT', 'ACTIVE');

INSERT IGNORE INTO `user_address` (`id`, `user_id`, `receiver_name`, `receiver_phone`, `province`, `city`, `district`, `detail_address`, `is_default`) VALUES
(1, 1, '林同学', '13000000000', '上海市', '上海市', '浦东新区', 'MiniPay 路 100 号', 1);

INSERT IGNORE INTO `user_account` (`id`, `user_id`, `balance`, `status`) VALUES
(1, 1, 10000.00, 'NORMAL'),
(2, 2, 0.00, 'NORMAL');

USE minipay_product;

CREATE TABLE IF NOT EXISTS `product_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT 0,
  `name` varchar(64) NOT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `status` varchar(32) NOT NULL DEFAULT 'NORMAL',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `merchant_id` bigint DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  `title` varchar(128) NOT NULL,
  `description` text,
  `main_image` varchar(500) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'ON_SALE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `product_sku` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `sku_name` varchar(128) NOT NULL,
  `attributes_json` varchar(512) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `original_price` decimal(10,2) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'ON_SALE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `product_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `image_url` varchar(500) NOT NULL,
  `sort` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO `product_category` (`id`, `parent_id`, `name`, `sort`, `status`) VALUES
(1, 0, '数码音频', 10, 'NORMAL'),
(2, 0, '办公外设', 20, 'NORMAL'),
(3, 0, '智能穿戴', 30, 'NORMAL'),
(4, 0, '充电配件', 40, 'NORMAL');

INSERT IGNORE INTO `product` (`id`, `merchant_id`, `category_id`, `title`, `description`, `main_image`, `status`) VALUES
(1001, 1, 1, 'MiniPods Pro 降噪耳机', '通勤、会议和运动都能使用的无线降噪耳机，支持快速配对和长续航。', 'https://images.unsplash.com/photo-1606220588913-b3aacb4d2f46?auto=format&fit=crop&w=900&q=80', 'ON_SALE'),
(1002, 1, 2, 'FlowKey 机械键盘', '紧凑配列机械键盘，适合开发、办公和游戏场景。', 'https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?auto=format&fit=crop&w=900&q=80', 'ON_SALE'),
(1003, 1, 3, 'Pulse Watch 智能手表', '支持运动记录、消息提醒和健康数据查看的日常智能手表。', 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80', 'ON_SALE'),
(1004, 1, 4, 'Orbit Desk 桌面充电站', '多设备桌面充电站，适合手机、耳机、手表同时充电。', 'https://images.unsplash.com/photo-1616410011236-7a42121dd981?auto=format&fit=crop&w=900&q=80', 'ON_SALE');

INSERT IGNORE INTO `product_sku` (`id`, `product_id`, `sku_name`, `attributes_json`, `price`, `original_price`, `status`) VALUES
(20011, 1001, '曜石黑 / 标准版', '{"颜色":"曜石黑","版本":"标准版"}', 399.00, 499.00, 'ON_SALE'),
(20012, 1001, '云雾白 / 长续航版', '{"颜色":"云雾白","版本":"长续航版"}', 459.00, 559.00, 'ON_SALE'),
(20021, 1002, '银灰 / 茶轴', '{"颜色":"银灰","轴体":"茶轴"}', 329.00, 399.00, 'ON_SALE'),
(20022, 1002, '墨绿 / 红轴', '{"颜色":"墨绿","轴体":"红轴"}', 349.00, 429.00, 'ON_SALE'),
(20031, 1003, '星光银 / 42mm', '{"颜色":"星光银","尺寸":"42mm"}', 699.00, 799.00, 'ON_SALE'),
(20032, 1003, '深空灰 / 46mm', '{"颜色":"深空灰","尺寸":"46mm"}', 759.00, 899.00, 'ON_SALE'),
(20041, 1004, '白色 / 65W', '{"颜色":"白色","功率":"65W"}', 189.00, 239.00, 'ON_SALE'),
(20042, 1004, '黑色 / 100W', '{"颜色":"黑色","功率":"100W"}', 269.00, 329.00, 'ON_SALE');

INSERT IGNORE INTO `product_image` (`id`, `product_id`, `image_url`, `sort`) VALUES
(1, 1001, 'https://images.unsplash.com/photo-1606220588913-b3aacb4d2f46?auto=format&fit=crop&w=900&q=80', 1),
(2, 1002, 'https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?auto=format&fit=crop&w=900&q=80', 1),
(3, 1003, 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80', 1),
(4, 1004, 'https://images.unsplash.com/photo-1616410011236-7a42121dd981?auto=format&fit=crop&w=900&q=80', 1);

USE minipay_inventory;

CREATE TABLE IF NOT EXISTS `inventory` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sku_id` bigint NOT NULL,
  `total_stock` int NOT NULL DEFAULT 0,
  `available_stock` int NOT NULL DEFAULT 0,
  `locked_stock` int NOT NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `inventory_lock` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) NOT NULL,
  `sku_id` bigint NOT NULL,
  `quantity` int NOT NULL,
  `status` varchar(32) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_sku` (`order_no`, `sku_id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO `inventory` (`sku_id`, `total_stock`, `available_stock`, `locked_stock`, `version`) VALUES
(20011, 86, 86, 0, 0),
(20012, 42, 42, 0, 0),
(20021, 55, 55, 0, 0),
(20022, 31, 31, 0, 0),
(20031, 64, 64, 0, 0),
(20032, 28, 28, 0, 0),
(20041, 120, 120, 0, 0),
(20042, 76, 76, 0, 0);

USE minipay_cart;

CREATE TABLE IF NOT EXISTS `cart_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `sku_id` bigint NOT NULL,
  `quantity` int NOT NULL,
  `selected` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

USE minipay_order;

CREATE TABLE IF NOT EXISTS `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` varchar(64) DEFAULT NULL,
  `order_no` varchar(64) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `amount` decimal(10,2) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `total_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `discount_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `freight_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `pay_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `status` varchar(32) NOT NULL DEFAULT 'CREATED',
  `receiver_name` varchar(64) DEFAULT NULL,
  `receiver_phone` varchar(32) DEFAULT NULL,
  `receiver_address` varchar(255) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `paid_at` datetime DEFAULT NULL,
  `cancelled_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) NOT NULL,
  `product_id` bigint NOT NULL,
  `sku_id` bigint NOT NULL,
  `product_title` varchar(128) NOT NULL,
  `sku_name` varchar(128) DEFAULT NULL,
  `product_image` varchar(500) DEFAULT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `quantity` int NOT NULL,
  `total_amount` decimal(10,2) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `order_status_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) NOT NULL,
  `from_status` varchar(32) DEFAULT NULL,
  `to_status` varchar(32) NOT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

USE minipay_payment;

CREATE TABLE IF NOT EXISTS `payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `payment_id` varchar(64) NOT NULL,
  `order_id` varchar(64) NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `trade_no` varchar(128) DEFAULT NULL,
  `paid_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_id` (`payment_id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `payment_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `payment_no` varchar(64) NOT NULL,
  `order_no` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL DEFAULT 1,
  `pay_amount` decimal(10,2) NOT NULL,
  `pay_channel` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'WAITING',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `paid_at` datetime DEFAULT NULL,
  `closed_at` datetime DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `payment_flow` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `payment_no` varchar(64) NOT NULL,
  `order_no` varchar(64) NOT NULL,
  `channel_trade_no` varchar(128) DEFAULT NULL,
  `pay_channel` varchar(32) NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `status` varchar(32) NOT NULL,
  `request_body` text,
  `response_body` text,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_payment_no` (`payment_no`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `refund_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `refund_no` varchar(64) NOT NULL,
  `payment_no` varchar(64) NOT NULL,
  `order_no` varchar(64) NOT NULL,
  `refund_amount` decimal(10,2) NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'CREATED',
  `reason` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_payment_no` (`payment_no`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

USE minipay_promotion;

CREATE TABLE IF NOT EXISTS `coupon` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `discount_amount` decimal(10,2) NOT NULL,
  `threshold_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `user_coupon` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `coupon_id` bigint NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'UNUSED',
  `received_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `used_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `promotion_activity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `activity_type` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO `coupon` (`id`, `name`, `discount_amount`, `threshold_amount`, `status`) VALUES
(1, '满500减30', 30.00, 500.00, 'ACTIVE'),
(2, '新人立减20', 20.00, 100.00, 'ACTIVE');

USE minipay_logistics;

CREATE TABLE IF NOT EXISTS `logistics_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) NOT NULL,
  `logistics_no` varchar(64) DEFAULT NULL,
  `carrier_name` varchar(64) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'CREATED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_logistics_no` (`logistics_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `logistics_trace` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `logistics_no` varchar(64) NOT NULL,
  `trace_content` varchar(255) NOT NULL,
  `trace_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_logistics_no` (`logistics_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

USE minipay_merchant;

CREATE TABLE IF NOT EXISTS `merchant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `merchant_name` varchar(128) NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'NORMAL',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `shop` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `merchant_id` bigint NOT NULL,
  `shop_name` varchar(128) NOT NULL,
  `logo` varchar(500) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'NORMAL',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `merchant_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `merchant_id` bigint NOT NULL,
  `balance` decimal(10,2) NOT NULL DEFAULT 0.00,
  `status` varchar(32) NOT NULL DEFAULT 'NORMAL',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `settlement_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `settlement_no` varchar(64) NOT NULL,
  `merchant_id` bigint NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'CREATED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settlement_no` (`settlement_no`),
  KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO `merchant` (`id`, `user_id`, `merchant_name`, `status`) VALUES
(1, 1, 'MiniPay 数码旗舰店', 'NORMAL');

INSERT IGNORE INTO `shop` (`id`, `merchant_id`, `shop_name`, `logo`, `status`) VALUES
(1, 1, 'MiniPay 数码旗舰店', NULL, 'NORMAL');

INSERT IGNORE INTO `merchant_account` (`id`, `merchant_id`, `balance`, `status`) VALUES
(1, 1, 0.00, 'NORMAL');

SET FOREIGN_KEY_CHECKS = 1;
