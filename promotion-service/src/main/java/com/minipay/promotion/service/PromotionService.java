package com.minipay.promotion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minipay.promotion.mapper.CouponMapper;
import com.minipay.promotion.model.Coupon;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PromotionService {
    @Resource
    private CouponMapper couponMapper;

    /**
     * 查询优惠券列表
     * @return 优惠券列表
     */
    public List<Coupon> listCoupons() {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupon::getStatus, "ACTIVE").orderByDesc(Coupon::getDiscountAmount);
        return couponMapper.selectList(wrapper);
    }

    /**
     * 领取优惠券
     * @param couponId 优惠券 ID
     * @return 领取结果
     */
    public Object receiveCoupon(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new IllegalArgumentException("优惠券不存在");
        }
        if (!"ACTIVE".equals(coupon.getStatus())) {
            throw new IllegalStateException("优惠券不可领取");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("couponId", couponId);
        result.put("name", coupon.getName());
        result.put("status", "RECEIVED");
        result.put("message", "优惠券领取成功");
        return result;
    }

    /**
     * 价格计算
     * @param req 计算请求
     * @return 计算结果
     */
    @SuppressWarnings("unchecked")
    public Object calculate(Object req) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        Long couponId = null;
        if (req instanceof Map<?, ?> map) {
            Object amount = map.get("totalAmount");
            if (amount == null) {
                amount = map.get("amount");
            }
            totalAmount = toBigDecimal(amount);
            Object couponValue = map.get("couponId");
            if (couponValue instanceof Number number) {
                couponId = number.longValue();
            }
        }
        //  使用最优优惠券计算折扣金额
        Coupon coupon = couponId == null ? bestCoupon(totalAmount) : couponMapper.selectById(couponId);
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (coupon != null && "ACTIVE".equals(coupon.getStatus()) && totalAmount.compareTo(coupon.getThresholdAmount()) >= 0) {
            discountAmount = coupon.getDiscountAmount();
        }
        // 如果待支付金额<0则改为0
        BigDecimal payAmount = totalAmount.subtract(discountAmount);
        if (payAmount.compareTo(BigDecimal.ZERO) < 0) {
            payAmount = BigDecimal.ZERO;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalAmount", totalAmount);
        result.put("couponId", coupon == null ? null : coupon.getId());
        result.put("discountAmount", discountAmount);
        result.put("payAmount", payAmount);
        return result;
    }

    /**
     * 查询最优优惠券
     * @param totalAmount 总金额
     * @return 最优优惠券
     */
    private Coupon bestCoupon(BigDecimal totalAmount) {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupon::getStatus, "ACTIVE")
                // 条件2：优惠券门槛金额 ≤ 订单总价（满减门槛满足）
                .le(Coupon::getThresholdAmount, totalAmount)
                // 按优惠金额 倒序排序，优惠最多的排在第一条
                .orderByDesc(Coupon::getDiscountAmount)
                // 拼接原生sql，只查询第一条数据
                .last("limit 1");
        return couponMapper.selectOne(wrapper);
    }

    /**
     * 将对象转换为 BigDecimal
     * @param value 要转换的值
     * @return BigDecimal
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }
}
