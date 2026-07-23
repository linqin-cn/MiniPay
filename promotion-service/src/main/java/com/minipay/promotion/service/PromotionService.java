package com.minipay.promotion.service;

import com.minipay.promotion.model.Coupon;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class PromotionService {
    public List<Coupon> listCoupons() { return Collections.emptyList(); }
    public Object receiveCoupon(Long couponId) { return null; }
    public Object calculate(Object req) { return null; }
}
