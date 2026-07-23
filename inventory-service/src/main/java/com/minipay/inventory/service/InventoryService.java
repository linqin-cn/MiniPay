package com.minipay.inventory.service;

import com.minipay.inventory.dto.InventoryReq;
import com.minipay.inventory.model.Inventory;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {
    public Inventory getInventory(Long skuId) { return null; }
    public Object lock(InventoryReq req) { return null; }
    public Object deduct(InventoryReq req) { return null; }
    public Object release(InventoryReq req) { return null; }
}
