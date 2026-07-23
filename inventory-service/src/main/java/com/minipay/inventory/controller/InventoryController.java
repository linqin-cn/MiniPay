package com.minipay.inventory.controller;

import com.minipay.common.resp.CommonResp;
import com.minipay.inventory.dto.InventoryReq;
import com.minipay.inventory.model.Inventory;
import com.minipay.inventory.service.InventoryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    @Resource
    private InventoryService inventoryService;

    @GetMapping("/skus/{skuId}")
    public CommonResp<Inventory> getInventory(@PathVariable Long skuId) { return new CommonResp<>(200, "TODO", inventoryService.getInventory(skuId), true); }

    @PostMapping("/lock")
    public CommonResp<Object> lock(@RequestBody InventoryReq req) { return new CommonResp<>(200, "TODO", inventoryService.lock(req), true); }

    @PostMapping("/deduct")
    public CommonResp<Object> deduct(@RequestBody InventoryReq req) { return new CommonResp<>(200, "TODO", inventoryService.deduct(req), true); }

    @PostMapping("/release")
    public CommonResp<Object> release(@RequestBody InventoryReq req) { return new CommonResp<>(200, "TODO", inventoryService.release(req), true); }

    @GetMapping("/health")
    public CommonResp<String> health() { return new CommonResp<>(200, "success", "inventory-service is running", true); }
}
