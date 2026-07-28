package com.minipay.order.mq;

import com.minipay.common.mq.PaymentSucceededEvent;
import com.minipay.order.service.OrderService;
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
        topic = "${minipay.mq.topics.payment-succeeded:payment-succeeded-topic}",
        consumerGroup = "${minipay.mq.consumer-groups.order-payment-succeeded:order-payment-succeeded-group}"
)
public class PaymentSucceededListener implements RocketMQListener<PaymentSucceededEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentSucceededListener.class);

    @Resource
    private OrderService orderService;

    @Override
    public void onMessage(PaymentSucceededEvent event) {
        if (event == null || event.getOrderNo() == null || event.getOrderNo().isBlank()) {
            LOG.warn("收到无效支付成功事件: {}", event);
            return;
        }
        LOG.info("收到支付成功MQ事件, orderNo: {}, paymentNo: {}", event.getOrderNo(), event.getPaymentNo());
        orderService.handlePaymentSucceeded(event.getOrderNo());
    }
}
