package com.minipay.payment.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单实体类
 */
@Data
@TableName("payment_order")
public class PaymentOrder {
    // id、支付单号、订单号、用户id、支付金额、支付渠道、支付状态、创建时间、支付时间、关闭时间、更新时间
    @TableId(type = IdType.AUTO)
    private Long id;
    private String paymentNo;
    private String orderNo;
    private Long userId;
    private BigDecimal payAmount;
    private String payChannel;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime closedAt;
    private LocalDateTime updatedAt;
}
