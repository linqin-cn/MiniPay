package com.minipay.inventory.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存实体类
 */
@Data
@TableName("inventory")
public class Inventory {
    // id为主键，策略为数据库自增
    @TableId(type = IdType.AUTO)
    private Long id;
    // skuId为商品具体型号id
    private Long skuId;
    // 库存总量
    private Integer totalStock;
    // 可用库存
    private Integer availableStock;
    // 锁定库存
    private Integer lockedStock;
    // 乐观锁版本号
    private Integer version;
    // 创建时间
    private LocalDateTime createdAt;
    // 更新时间
    private LocalDateTime updatedAt;
}
