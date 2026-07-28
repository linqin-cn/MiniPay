package com.minipay.logistics.mq;

import com.minipay.common.mq.OrderShippedEvent;
import com.minipay.logistics.service.LogisticsService;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "minipay.mq", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = "${minipay.mq.topics.order-shipped:order-shipped-topic}",
        consumerGroup = "${minipay.mq.consumer-groups.logistics-order-shipped:logistics-order-shipped-group}"
)
public class OrderShippedListener implements RocketMQListener<OrderShippedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(OrderShippedListener.class);

    @Resource
    private LogisticsService logisticsService;

    @Override
    public void onMessage(OrderShippedEvent event) {
        if (event == null || event.getOrderNo() == null || event.getOrderNo().isBlank()) {
            LOG.warn("收到无效订单发货事件: {}", event);
            return;
        }
        LOG.info("收到订单发货MQ事件，准备创建物流单, orderNo: {}", event.getOrderNo());
        Map<String, Object> req = new HashMap<>();
        req.put("orderNo", event.getOrderNo());
        req.put("carrierName", "MiniPay Express");
        req.put("status", "SHIPPED");
        logisticsService.createLogistics(req);
    }
}
