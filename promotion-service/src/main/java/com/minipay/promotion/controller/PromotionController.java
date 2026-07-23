package com.minipay.promotion.controller;

import com.minipay.common.resp.CommonResp;
import com.minipay.promotion.model.Coupon;
import com.minipay.promotion.service.PromotionService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {
    @Resource
    private PromotionService promotionService;

    @GetMapping("/coupons")
    public CommonResp<List<Coupon>> listCoupons() { return new CommonResp<>(200, "TODO", promotionService.listCoupons(), true); }

    @PostMapping("/coupons/{couponId}/receive")
    public CommonResp<Object> receiveCoupon(@PathVariable Long couponId) { return new CommonResp<>(200, "TODO", promotionService.receiveCoupon(couponId), true); }

    @PostMapping("/calculate")
    public CommonResp<Object> calculate(@RequestBody Object req) { return new CommonResp<>(200, "TODO", promotionService.calculate(req), true); }

    @GetMapping("/health")
    public CommonResp<String> health() { return new CommonResp<>(200, "success", "promotion-service is running", true); }
}
