package com.minipay.common.enums;

/**
 * 支付状态枚举
 */
public enum PaymentStatus {
    WAITING, // 等待支付
    PROCESSING, // 支付处理中
    SUCCESS, // 支付成功
    FAILED, // 支付失败
    CLOSED, // 支付已关闭
    REFUNDING, // 支付退款中
    REFUNDED // 支付已退款
}
