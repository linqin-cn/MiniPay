package com.minipay.payment.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水实体类
 */
@Data
@TableName("payment_flow")
public class PaymentFlow {
    // id、支付流水号、订单号、渠道交易号、支付渠道、金额、状态、请求体、响应体、创建时间
    @TableId(type = IdType.AUTO)
    private Long id;
    private String paymentNo;
    private String orderNo;
    private String channelTradeNo;
    private String payChannel;
    private BigDecimal amount;
    private String status;
    private String requestBody;
    private String responseBody;
    private LocalDateTime createdAt;
}
