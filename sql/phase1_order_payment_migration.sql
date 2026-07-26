SET NAMES utf8mb4;

USE minipay_order;

ALTER TABLE `orders`
  ADD COLUMN IF NOT EXISTS `order_no` varchar(64) DEFAULT NULL AFTER `order_id`,
  ADD COLUMN IF NOT EXISTS `user_id` bigint DEFAULT NULL AFTER `order_no`,
  ADD COLUMN IF NOT EXISTS `total_amount` decimal(10,2) NOT NULL DEFAULT 0.00 AFTER `description`,
  ADD COLUMN IF NOT EXISTS `discount_amount` decimal(10,2) NOT NULL DEFAULT 0.00 AFTER `total_amount`,
  ADD COLUMN IF NOT EXISTS `freight_amount` decimal(10,2) NOT NULL DEFAULT 0.00 AFTER `discount_amount`,
  ADD COLUMN IF NOT EXISTS `pay_amount` decimal(10,2) NOT NULL DEFAULT 0.00 AFTER `freight_amount`,
  ADD COLUMN IF NOT EXISTS `receiver_name` varchar(64) DEFAULT NULL AFTER `status`,
  ADD COLUMN IF NOT EXISTS `receiver_phone` varchar(32) DEFAULT NULL AFTER `receiver_name`,
  ADD COLUMN IF NOT EXISTS `receiver_address` varchar(255) DEFAULT NULL AFTER `receiver_phone`,
  ADD COLUMN IF NOT EXISTS `remark` varchar(255) DEFAULT NULL AFTER `receiver_address`,
  ADD COLUMN IF NOT EXISTS `paid_at` datetime DEFAULT NULL AFTER `created_at`,
  ADD COLUMN IF NOT EXISTS `cancelled_at` datetime DEFAULT NULL AFTER `paid_at`,
  ADD COLUMN IF NOT EXISTS `completed_at` datetime DEFAULT NULL AFTER `cancelled_at`;

UPDATE `orders`
SET `order_no` = `order_id`,
    `total_amount` = IFNULL(`amount`, 0.00),
    `pay_amount` = IFNULL(`amount`, 0.00)
WHERE `order_no` IS NULL;

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
  KEY `idx_order_no` (`order_no`),
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
