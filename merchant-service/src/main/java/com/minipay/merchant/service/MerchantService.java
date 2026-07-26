package com.minipay.merchant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minipay.merchant.mapper.MerchantMapper;
import com.minipay.merchant.mapper.ShopMapper;
import com.minipay.merchant.model.Merchant;
import com.minipay.merchant.model.Shop;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class MerchantService {
    private static final String ORDER_SERVICE_URL = "http://localhost:8081/api/orders";
    private static final Long DEMO_USER_ID = 1L; // 用于演示的默认用户ID

    @Resource
    private MerchantMapper merchantMapper;

    @Resource
    private ShopMapper shopMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    // 商家入驻方法，接收一个请求对象，返回商家实体
    @SuppressWarnings("unchecked")
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
    public Merchant getMerchant(Long id) {
        return merchantMapper.selectById(id);
    }

    /**
     * 创建店铺
     * @param req 请求对象
     * @return Shop 店铺实体
     */
    @SuppressWarnings("unchecked")
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
            // 使用 RestTemplate 发送 GET 请求 调用远程订单服务接口，把接口返回的响应 JSON 数据，封装为 Object 对象并返回。
            return restTemplate.getForObject(ORDER_SERVICE_URL, Object.class);
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
    public Object shipOrder(String orderNo) {
        try {
            // 转发请求，第二个参数：null → POST 请求的请求体 (RequestBody)
            return restTemplate.postForObject(ORDER_SERVICE_URL + "/" + orderNo + "/ship", null, Object.class);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("message", "发货失败或订单服务暂不可用");
            result.put("orderNo", orderNo);
            result.put("error", e.getMessage());
            return result;
        }
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
