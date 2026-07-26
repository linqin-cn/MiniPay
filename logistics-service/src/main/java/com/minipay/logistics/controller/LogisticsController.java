package com.minipay.logistics.controller;

import com.minipay.common.resp.CommonResp;
import com.minipay.logistics.model.LogisticsOrder;
import com.minipay.logistics.service.LogisticsService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logistics")
public class LogisticsController {
    @Resource
    private LogisticsService logisticsService;

    // 创建物流订单
    @PostMapping
    public CommonResp<LogisticsOrder> createLogistics(@RequestBody Object req) { return new CommonResp<>(200, "创建物流单成功", logisticsService.createLogistics(req), true); }

    // 查询物流订单
    @GetMapping("/orders/{orderNo}")
    public CommonResp<LogisticsOrder> getByOrderNo(@PathVariable String orderNo) { return new CommonResp<>(200, "查询物流单成功", logisticsService.getByOrderNo(orderNo), true); }

    // 出查询物流轨迹
    @GetMapping("/{logisticsNo}/trace")
    public CommonResp<Object> trace(@PathVariable String logisticsNo) { return new CommonResp<>(200, "查询物流轨迹成功", logisticsService.trace(logisticsNo), true); }
}
