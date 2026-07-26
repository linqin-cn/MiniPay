package com.minipay.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minipay.inventory.dto.InventoryReq;
import com.minipay.inventory.mapper.InventoryLockMapper;
import com.minipay.inventory.mapper.InventoryMapper;
import com.minipay.inventory.model.Inventory;
import com.minipay.inventory.model.InventoryLock;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class InventoryService {
    @Resource
    private InventoryMapper inventoryMapper;

    @Resource
    private InventoryLockMapper inventoryLockMapper;

    // 根据skuId去数据库查询该商品的库存信息（不只数量）
    public Inventory getInventory(Long skuId) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getSkuId, skuId);
        return inventoryMapper.selectOne(wrapper);
    }

    // 锁定库存，事务
    @Transactional
    public Object lock(InventoryReq req) {
        // 检验请求参数是否合法
        validateReq(req);
        // 获取该商品的库存信息
        Inventory inventory = requireInventory(req.getSkuId());
        // 可以库存为空，或库存数少去请求数 -- 库存不足
        if (inventory.getAvailableStock() == null || inventory.getAvailableStock() < req.getQuantity()) {
            throw new IllegalStateException("库存不足");
        }
        // 查看是否已经存在锁定记录，如果存在且状态为LOCKED，则直接返回锁定成功的结果
        InventoryLock existing = findLock(req.getOrderNo(), req.getSkuId());
        if (existing != null && "LOCKED".equals(existing.getStatus())) {
            // 返回哈希表--data
            return result("LOCKED", existing.getOrderNo(), existing.getSkuId(), existing.getQuantity());
        }
        // 锁定流程： 库存信息减去可用库存数
        inventory.setAvailableStock(inventory.getAvailableStock() - req.getQuantity());
        // 已锁定库存增加锁定数量
        inventory.setLockedStock(safeInt(inventory.getLockedStock()) + req.getQuantity());
        // 更新时间
        inventory.setUpdatedAt(LocalDateTime.now());
        // 数据库更新
        inventoryMapper.updateById(inventory);

        // 创建库存锁定记录，状态为LOCKED，插入数据库
        InventoryLock lock = new InventoryLock();
        lock.setOrderNo(req.getOrderNo());
        lock.setSkuId(req.getSkuId());
        lock.setQuantity(req.getQuantity());
        lock.setStatus("LOCKED");
        lock.setCreatedAt(LocalDateTime.now());
        lock.setUpdatedAt(LocalDateTime.now());
        inventoryLockMapper.insert(lock);
        // 返回结果，哈希表
        return result("LOCKED", req.getOrderNo(), req.getSkuId(), req.getQuantity());
    }

    @Transactional
    public Object deduct(InventoryReq req) {
        validateReq(req);
        // 寻找lock对象，如果不存在则抛出异常
        InventoryLock lock = requireLock(req.getOrderNo(), req.getSkuId());
        // 如果状态已经是DEDUCTED，则直接返回扣减成功的结果
        if ("DEDUCTED".equals(lock.getStatus())) {
            return result("DEDUCTED", req.getOrderNo(), req.getSkuId(), lock.getQuantity());
        }
        // 如果状态不是LOCKED，则抛出异常，表示库存锁定状态不允许扣减
        if (!"LOCKED".equals(lock.getStatus())) {
            throw new IllegalStateException("库存锁定状态不允许扣减：" + lock.getStatus());
        }
        // 是锁定状态才可以开始扣减
        Inventory inventory = requireInventory(req.getSkuId());
        inventory.setTotalStock(safeInt(inventory.getTotalStock()) - lock.getQuantity());
        inventory.setLockedStock(Math.max(0, safeInt(inventory.getLockedStock()) - lock.getQuantity()));
        inventory.setUpdatedAt(LocalDateTime.now());
        inventoryMapper.updateById(inventory);

        // 设置库存锁定实体类的锁定记录的状态为DEDUCTED，并更新数据库
        lock.setStatus("DEDUCTED");
        lock.setUpdatedAt(LocalDateTime.now());
        inventoryLockMapper.updateById(lock);
        return result("DEDUCTED", req.getOrderNo(), req.getSkuId(), lock.getQuantity());
    }

    @Transactional
    public Object release(InventoryReq req) {
        // 检验请求参数是否合法
        validateReq(req);
        // 寻找lock对象，如果不存在则抛出异常
        InventoryLock lock = requireLock(req.getOrderNo(), req.getSkuId());
        // 如果该锁对象的状态已经是RELEASED，则直接返回释放成功的结果
        if ("RELEASED".equals(lock.getStatus())) {
            return result("RELEASED", req.getOrderNo(), req.getSkuId(), lock.getQuantity());
        }
        // 如果该锁对象的状态不是LOCKED，则直接返回当前状态的结果
        if (!"LOCKED".equals(lock.getStatus())) {
            return result(lock.getStatus(), req.getOrderNo(), req.getSkuId(), lock.getQuantity());
        }
        // 锁状态是LOCKED才可以释放库存，释放库存的流程：可用库存数增加锁定数量，已锁定库存减少锁定数量
        Inventory inventory = requireInventory(req.getSkuId());
        inventory.setAvailableStock(safeInt(inventory.getAvailableStock()) + lock.getQuantity());
        inventory.setLockedStock(Math.max(0, safeInt(inventory.getLockedStock()) - lock.getQuantity()));
        inventory.setUpdatedAt(LocalDateTime.now());
        inventoryMapper.updateById(inventory);

        lock.setStatus("RELEASED");
        lock.setUpdatedAt(LocalDateTime.now());
        inventoryLockMapper.updateById(lock);
        return result("RELEASED", req.getOrderNo(), req.getSkuId(), lock.getQuantity());
    }

    // 根据skuId获取其库存信息
    private Inventory requireInventory(Long skuId) {
        Inventory inventory = getInventory(skuId);
        if (inventory == null) {
            throw new IllegalArgumentException("库存不存在，skuId=" + skuId);
        }
        return inventory;
    }

    // 寻找锁定记录，如果不存在则抛出异常
    private InventoryLock requireLock(String orderNo, Long skuId) {
        InventoryLock lock = findLock(orderNo, skuId);
        if (lock == null) {
            throw new IllegalArgumentException("库存锁定记录不存在");
        }
        return lock;
    }

    // 寻找锁定记录，如果存在则返回该记录，否则返回null
    private InventoryLock findLock(String orderNo, Long skuId) {
        LambdaQueryWrapper<InventoryLock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryLock::getOrderNo, orderNo).eq(InventoryLock::getSkuId, skuId).orderByDesc(InventoryLock::getCreatedAt);
        return inventoryLockMapper.selectOne(wrapper.last("limit 1"));
    }

    // 验证库存请求参数:订单号、skuId、数量不能为空，且数量必须大于0
    private void validateReq(InventoryReq req) {
        if (req == null || req.getOrderNo() == null || req.getOrderNo().isEmpty() || req.getSkuId() == null || req.getQuantity() == null || req.getQuantity() <= 0) {
            throw new IllegalArgumentException("订单号、SKU 和数量不能为空，且数量必须大于0");
        }
    }

    // 安全地处理Integer类型的值，如果为null则返回0，否则返回原值
    private Integer safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    // 返回库存操作结果的Map对象，包含状态、订单号、skuId和数量
    private Map<String, Object> result(String status, String orderNo, Long skuId, Integer quantity) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        map.put("orderNo", orderNo);
        map.put("skuId", skuId);
        map.put("quantity", quantity);
        return map;
    }
}
