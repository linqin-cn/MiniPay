package com.minipay.order.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单状态日志模型
 */
@Data
@TableName("order_status_log")
public class OrderStatusLog {
    // 订单id、订单号、原状态、目标状态、备注、创建时间
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private String fromStatus;
    private String toStatus;
    private String remark;
    private LocalDateTime createdAt;
}
