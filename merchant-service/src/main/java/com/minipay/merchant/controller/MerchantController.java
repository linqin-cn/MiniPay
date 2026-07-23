package com.minipay.merchant.controller;

import com.minipay.common.resp.CommonResp;
import com.minipay.merchant.model.Merchant;
import com.minipay.merchant.model.Shop;
import com.minipay.merchant.service.MerchantService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {
    @Resource
    private MerchantService merchantService;

    @PostMapping("/register")
    public CommonResp<Merchant> register(@RequestBody Object req) { return new CommonResp<>(200, "TODO", merchantService.register(req), true); }

    @GetMapping("/{id}")
    public CommonResp<Merchant> getMerchant(@PathVariable Long id) { return new CommonResp<>(200, "TODO", merchantService.getMerchant(id), true); }

    @PostMapping("/shops")
    public CommonResp<Shop> createShop(@RequestBody Object req) { return new CommonResp<>(200, "TODO", merchantService.createShop(req), true); }

    @GetMapping("/orders")
    public CommonResp<Object> listOrders() { return new CommonResp<>(200, "TODO", merchantService.listOrders(), true); }

    @PostMapping("/orders/{orderNo}/ship")
    public CommonResp<Object> shipOrder(@PathVariable String orderNo) { return new CommonResp<>(200, "TODO", merchantService.shipOrder(orderNo), true); }

    @GetMapping("/health")
    public CommonResp<String> health() { return new CommonResp<>(200, "success", "merchant-service is running", true); }
}
