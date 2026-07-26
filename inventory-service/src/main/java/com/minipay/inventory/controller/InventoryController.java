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

    // 查询库存
    @GetMapping("/skus/{skuId}")
    public CommonResp<Inventory> getInventory(@PathVariable Long skuId) { return new CommonResp<>(200, "查询库存成功", inventoryService.getInventory(skuId), true); }

    // 锁定库存
    @PostMapping("/lock")
    public CommonResp<Object> lock(@RequestBody InventoryReq req) { return new CommonResp<>(200, "锁定库存成功", inventoryService.lock(req), true); }

    // 扣减库存
    @PostMapping("/deduct")
    public CommonResp<Object> deduct(@RequestBody InventoryReq req) { return new CommonResp<>(200, "扣减库存成功", inventoryService.deduct(req), true); }

    // 释放库存
    @PostMapping("/release")
    public CommonResp<Object> release(@RequestBody InventoryReq req) { return new CommonResp<>(200, "释放库存成功", inventoryService.release(req), true); }
}
