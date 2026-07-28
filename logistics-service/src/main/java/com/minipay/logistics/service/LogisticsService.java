package com.minipay.logistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minipay.logistics.mapper.LogisticsOrderMapper;
import com.minipay.logistics.model.LogisticsOrder;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LogisticsService {
    @Resource
    private LogisticsOrderMapper logisticsOrderMapper;

    /**
     * 创建物流订单
     * @param req 请求参数
     * @return 物流订单
     */
    @CacheEvict(cacheNames = {"logistics:order", "logistics:trace"}, allEntries = true)
    public LogisticsOrder createLogistics(Object req) {
        // 创建物流订单时，✅ 如果 req 是 Map 的实例：强制转为 Map<String, Object> 赋值给 map
        //❌ 如果 req 不是 Map：直接新建一个空的 HashMap 赋值
        Map<String, Object> map = req instanceof Map<?, ?> ? (Map<String, Object>) req : new HashMap<>();
        String orderNo = stringValue(map.get("orderNo"));
        if (orderNo == null || orderNo.isEmpty()) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        // 查询数据库中是否已经存在该订单号的物流订单，如果存在则直接返回该订单
        LogisticsOrder existing = getByOrderNo(orderNo);
        if (existing != null) {
            return existing;
        }
        // 新建物流订单
        LogisticsOrder order = new LogisticsOrder();
        order.setOrderNo(orderNo);
        order.setLogisticsNo(generateLogisticsNo());
        // 如果 map 中没有 carrierName，则默认值为 "MiniPay Express"
        order.setCarrierName(defaultString(stringValue(map.get("carrierName")), "MiniPay Express"));
        order.setStatus(defaultString(stringValue(map.get("status")), "SHIPPED"));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        logisticsOrderMapper.insert(order);
        return order;
    }

    /**
     * 根据订单号查询物流订单
     * @param orderNo 订单号
     * @return 物流订单
     */
    @Cacheable(cacheNames = "logistics:order", key = "#orderNo", unless = "#result == null")
    public LogisticsOrder getByOrderNo(String orderNo) {
        LambdaQueryWrapper<LogisticsOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LogisticsOrder::getOrderNo, orderNo).orderByDesc(LogisticsOrder::getCreatedAt).last("limit 1");
        return logisticsOrderMapper.selectOne(wrapper);
    }

    /**
     * 查询物流轨迹
     * @param logisticsNo 物流单号
     * @return 物流轨迹
     */
    @Cacheable(cacheNames = "logistics:trace", key = "#logisticsNo")
    public Object trace(String logisticsNo) {
        LambdaQueryWrapper<LogisticsOrder> wrapper = new LambdaQueryWrapper<>();
        // where 条件：物流单号等于传入的 logisticsNo
        wrapper.eq(LogisticsOrder::getLogisticsNo, logisticsNo);
        LogisticsOrder order = logisticsOrderMapper.selectOne(wrapper);
        if (order == null) {
            throw new IllegalArgumentException("物流单不存在");
        }
        List<Map<String, Object>> traces = new ArrayList<>();
        traces.add(traceItem("CREATED", "商家已创建物流单", order.getCreatedAt()));
        traces.add(traceItem(order.getStatus(), "包裹运输中", order.getUpdatedAt()));
        return traces;
    }

    /**
     * 创建物流轨迹项
     * @param status 状态
     * @param content 内容
     * @param time 时间
     * @return 物流轨迹项
     */
    private Map<String, Object> traceItem(String status, String content, LocalDateTime time) {
        Map<String, Object> item = new HashMap<>();
        item.put("status", status);
        item.put("content", content);
        item.put("time", time);
        return item;
    }

    /**
     * 生成物流单号
     * @return 物流单号
     */
    private String generateLogisticsNo() {
        return "L" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * 将对象转换为字符串
     * @param value 对象
     * @return 字符串
     */
    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 获取默认字符串
     * @param value 原始字符串
     * @param defaultValue 默认字符串
     * @return 最终字符串
     */
    private String defaultString(String value, String defaultValue) {
        return value == null || value.isEmpty() ? defaultValue : value;
    }
}
