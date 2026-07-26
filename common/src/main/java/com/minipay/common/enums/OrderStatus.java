package com.minipay.common.enums;

/**
 * 订单状态枚举
 */
public enum OrderStatus {
    CREATED, // 订单已创建
    PAYING,  // 订单支付中
    PAID,    // 订单已支付
    SHIPPED, // 订单已发货
    RECEIVED, // 订单已收货
    COMPLETED, // 订单已完成
    CANCELLED, // 订单已取消
    CLOSED, // 订单已关闭
    REFUNDING, // 订单退款中
    REFUNDED // 订单已退款
}
