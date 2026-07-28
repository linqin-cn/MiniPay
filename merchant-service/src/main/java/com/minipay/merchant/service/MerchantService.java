package com.minipay.merchant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minipay.merchant.mapper.MerchantMapper;
import com.minipay.merchant.mapper.ShopMapper;
import com.minipay.merchant.model.Merchant;
import com.minipay.merchant.model.Shop;
import com.minipay.common.util.JwtUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class MerchantService {
    private static final Long DEMO_USER_ID = 1L; // 未登录演示兜底用户ID

    @Value("${minipay.services.order-url:http://localhost:8081/api/orders}")
    private String orderServiceUrl;

    @Resource
    private MerchantMapper merchantMapper;

    @Resource
    private ShopMapper shopMapper;

    @Resource
    private HttpServletRequest request;

    private final RestTemplate restTemplate = new RestTemplate();

    // 商家入驻方法，接收一个请求对象，返回商家实体
    @SuppressWarnings("unchecked")
    @CacheEvict(cacheNames = {"merchant:detail", "merchant:current", "merchant:orders"}, allEntries = true)
    public Merchant register(Object req) {
        Map<String, Object> map = req instanceof Map<?, ?> ? (Map<String, Object>) req : new HashMap<>();
        // 获取用户id
        Long userId = toLong(map.get("userId"), DEMO_USER_ID);
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        // last()：作用是拼接在整条 SQL 语句的最末尾
        wrapper.eq(Merchant::getUserId, userId).last("limit 1");
        Merchant existing = merchantMapper.selectOne(wrapper);
        if (existing != null) {
            return existing;
        }
        // 新建商家实体，设置属性并插入数据库
        Merchant merchant = new Merchant();
        merchant.setUserId(userId);
        merchant.setMerchantName(defaultString(stringValue(map.get("merchantName")), "MiniPay 默认商家"));
        merchant.setStatus("ACTIVE");
        merchant.setCreatedAt(LocalDateTime.now());
        merchant.setUpdatedAt(LocalDateTime.now());
        merchantMapper.insert(merchant);
        return merchant;
    }

    // 查询商家信息方法，根据商家ID返回商家实体
    @Cacheable(cacheNames = "merchant:detail", key = "#id", unless = "#result == null")
    public Merchant getMerchant(Long id) {
        return merchantMapper.selectById(id);
    }

    /**
     * 获取当前登录用户对应的商家；如果还没有商家记录，则自动创建一条。
     */
    @Cacheable(cacheNames = "merchant:current", key = "#root.target.currentUserId()", unless = "#result == null")
    public Merchant getCurrentMerchant() {
        return findOrCreateMerchantByUserId(currentUserId());
    }

    /**
     * 创建店铺
     * @param req 请求对象
     * @return Shop 店铺实体
     */
    @SuppressWarnings("unchecked")
    @CacheEvict(cacheNames = {"merchant:detail", "merchant:current", "merchant:orders"}, allEntries = true)
    public Shop createShop(Object req) {
        Map<String, Object> map = req instanceof Map<?, ?> ? (Map<String, Object>) req : new HashMap<>();
        Long merchantId = toLong(map.get("merchantId"), null);
        // 如果 merchantId 为 null，则调用 register 方法注册商家，并获取新商家的 ID
        if (merchantId == null) {
            Merchant merchant = register(req);
            merchantId = merchant.getId();
        }
        // 创建店铺
        Shop shop = new Shop();
        shop.setMerchantId(merchantId);
        shop.setShopName(defaultString(stringValue(map.get("shopName")), "MiniPay 官方店"));
        shop.setLogo(stringValue(map.get("logo")));
        shop.setStatus("OPEN");
        shopMapper.insert(shop);
        return shop;
    }

    /**
     * 列出商家的订单
     * @return 订单列表
     */
    public Object listOrders() {
        try {
            Long merchantId = getCurrentMerchant().getId();
            return restTemplate.getForObject(orderServiceUrl + "/merchant/" + merchantId, Object.class);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("message", "订单服务暂不可用");
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * 商家发货
     * @param orderNo 订单号
     * @return 发货结果
     */
    @CacheEvict(cacheNames = "merchant:orders", key = "#root.target.currentUserId()")
    public Object shipOrder(String orderNo) {
        try {
            // 转发请求，第二个参数：null → POST 请求的请求体 (RequestBody)
            return restTemplate.postForObject(orderServiceUrl + "/" + orderNo + "/ship", null, Object.class);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("message", "发货失败或订单服务暂不可用");
            result.put("orderNo", orderNo);
            result.put("error", e.getMessage());
            return result;
        }
    }

    private Merchant findDemoMerchant() {
        return findMerchantByUserId(DEMO_USER_ID);
    }

    private Merchant findMerchantByUserId(Long userId) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Merchant::getUserId, userId).last("limit 1");
        return merchantMapper.selectOne(wrapper);
    }

    private Merchant findOrCreateMerchantByUserId(Long userId) {
        Merchant merchant = findMerchantByUserId(userId);
        if (merchant != null) {
            return merchant;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("merchantName", "商家" + userId);
        return register(map);
    }

    public Long currentUserId() {
        String token = request.getHeader("token");
        Long userId = token == null || token.isEmpty() ? null : JwtUtil.getUserId(token);
        return userId == null ? DEMO_USER_ID : userId;
    }

    // 用户id转换为Long类型，如果value为null则返回defaultValue
    private Long toLong(Object value, Long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.isEmpty() ? defaultValue : value;
    }
}
