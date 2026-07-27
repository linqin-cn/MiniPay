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

    // 商家入驻
    @PostMapping("/register")
    public CommonResp<Merchant> register(@RequestBody Object req) { return new CommonResp<>(200, "商家入驻成功", merchantService.register(req), true); }

    // 查询商家信息
    @GetMapping("/{id}")
    public CommonResp<Merchant> getMerchant(@PathVariable Long id) { return new CommonResp<>(200, "查询商家成功", merchantService.getMerchant(id), true); }

    // 查询当前登录用户对应的商家信息
    @GetMapping("/me")
    public CommonResp<Merchant> getCurrentMerchant() { return new CommonResp<>(200, "查询当前商家成功", merchantService.getCurrentMerchant(), true); }

    // 创建店铺
    @PostMapping("/shops")
    public CommonResp<Shop> createShop(@RequestBody Object req) { return new CommonResp<>(200, "创建店铺成功", merchantService.createShop(req), true); }

    // 查询商家的订单列表
    @GetMapping("/orders")
    public CommonResp<Object> listOrders() { return new CommonResp<>(200, "查询商家订单成功", merchantService.listOrders(), true); }

    // 商家发货
    @PostMapping("/orders/{orderNo}/ship")
    public CommonResp<Object> shipOrder(@PathVariable String orderNo) { return new CommonResp<>(200, "商家发货成功", merchantService.shipOrder(orderNo), true); }
}
