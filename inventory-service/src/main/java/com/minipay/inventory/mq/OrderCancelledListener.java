package com.minipay.inventory.mq;

import com.minipay.common.mq.OrderCancelledEvent;
import com.minipay.common.mq.OrderItemMessage;
import com.minipay.inventory.dto.InventoryReq;
import com.minipay.inventory.service.InventoryService;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "minipay.mq", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = "${minipay.mq.topics.order-cancelled:order-cancelled-topic}",
        consumerGroup = "${minipay.mq.consumer-groups.inventory-order-cancelled:inventory-order-cancelled-group}"
)
public class OrderCancelledListener implements RocketMQListener<OrderCancelledEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(OrderCancelledListener.class);

    @Resource
    private InventoryService inventoryService;

    @Override
    public void onMessage(OrderCancelledEvent event) {
        if (event == null || event.getOrderNo() == null || event.getOrderNo().isBlank()) {
            LOG.warn("收到无效订单取消事件: {}", event);
            return;
        }
        LOG.info("收到订单取消MQ事件，准备释放库存, orderNo: {}", event.getOrderNo());
        for (OrderItemMessage item : event.getItems()) {
            if (item == null || item.getSkuId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }
            InventoryReq req = new InventoryReq();
            req.setOrderNo(event.getOrderNo());
            req.setSkuId(item.getSkuId());
            req.setQuantity(item.getQuantity());
            inventoryService.release(req);
        }
    }
}
