package com.minipay.order.mq;

import com.minipay.common.mq.OrderCancelledEvent;
import com.minipay.common.mq.OrderShippedEvent;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

    @Value("${minipay.mq.enabled:false}")
    private boolean mqEnabled;

    @Value("${minipay.mq.topics.order-cancelled:order-cancelled-topic}")
    private String orderCancelledTopic;

    @Value("${minipay.mq.topics.order-shipped:order-shipped-topic}")
    private String orderShippedTopic;

    public OrderEventPublisher(ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider) {
        this.rocketMQTemplateProvider = rocketMQTemplateProvider;
    }

    public boolean publishOrderCancelled(OrderCancelledEvent event) {
        return publish(orderCancelledTopic, event, "订单取消", event.getOrderNo());
    }

    public boolean publishOrderShipped(OrderShippedEvent event) {
        return publish(orderShippedTopic, event, "订单发货", event.getOrderNo());
    }

    private boolean publish(String topic, Object event, String eventName, String orderNo) {
        if (!mqEnabled) {
            return false;
        }
        RocketMQTemplate rocketMQTemplate = rocketMQTemplateProvider.getIfAvailable();
        if (rocketMQTemplate == null) {
            LOG.warn("RocketMQ未初始化，跳过{}事件发送, orderNo: {}", eventName, orderNo);
            return false;
        }
        try {
            rocketMQTemplate.convertAndSend(topic, event);
            LOG.info("已发送{}MQ事件, topic: {}, orderNo: {}", eventName, topic, orderNo);
            return true;
        } catch (Exception e) {
            LOG.warn("{}MQ事件发送失败, orderNo: {}, error: {}", eventName, orderNo, e.getMessage());
            return false;
        }
    }
}
