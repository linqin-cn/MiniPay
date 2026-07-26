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

    // 查询优惠券列表
    @GetMapping("/coupons")
    public CommonResp<List<Coupon>> listCoupons() { return new CommonResp<>(200, "查询优惠券成功", promotionService.listCoupons(), true); }

    // 领取优惠券
    @PostMapping("/coupons/{couponId}/receive")
    public CommonResp<Object> receiveCoupon(@PathVariable Long couponId) { return new CommonResp<>(200, "领取优惠券成功", promotionService.receiveCoupon(couponId), true); }

    // 价格计算
    @PostMapping("/calculate")
    public CommonResp<Object> calculate(@RequestBody Object req) { return new CommonResp<>(200, "优惠计算成功", promotionService.calculate(req), true); }
}
