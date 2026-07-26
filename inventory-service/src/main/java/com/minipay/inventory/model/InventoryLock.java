package com.minipay.inventory.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存锁定实体类
 */
@Data
@TableName("inventory_lock")
public class InventoryLock {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long skuId;
    private Integer quantity;
    // 锁定状态：LOCKED、UNLOCKED、DEDUCTED
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
