package com.minipay.payment.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_flow")
public class PaymentFlow {
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
