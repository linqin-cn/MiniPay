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

    @PostMapping
    public CommonResp<LogisticsOrder> createLogistics(@RequestBody Object req) { return new CommonResp<>(200, "TODO", logisticsService.createLogistics(req), true); }

    @GetMapping("/orders/{orderNo}")
    public CommonResp<LogisticsOrder> getByOrderNo(@PathVariable String orderNo) { return new CommonResp<>(200, "TODO", logisticsService.getByOrderNo(orderNo), true); }

    @GetMapping("/{logisticsNo}/trace")
    public CommonResp<Object> trace(@PathVariable String logisticsNo) { return new CommonResp<>(200, "TODO", logisticsService.trace(logisticsNo), true); }

    @GetMapping("/health")
    public CommonResp<String> health() { return new CommonResp<>(200, "success", "logistics-service is running", true); }
}
