package com.minipay.common.enums;

/**
 * 库存锁定状态枚举
 */
public enum InventoryLockStatus {
    LOCKED, // 库存已锁定
    DEDUCTED,// 库存已扣减
    RELEASED // 库存已释放
}
