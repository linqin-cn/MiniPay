package com.minipay.logistics.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物流订单实体类
 */
@Data
@TableName("logistics_order")
public class LogisticsOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    // 物流单号
    private String logisticsNo;
    // 承运商名称
    private String carrierName;
    // 状态
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
