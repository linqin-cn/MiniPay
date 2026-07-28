package com.minipay.payment.mq;

import com.minipay.common.mq.PaymentSucceededEvent;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

    @Value("${minipay.mq.enabled:false}")
    private boolean mqEnabled;

    @Value("${minipay.mq.topics.payment-succeeded:payment-succeeded-topic}")
    private String paymentSucceededTopic;

    public PaymentEventPublisher(ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider) {
        this.rocketMQTemplateProvider = rocketMQTemplateProvider;
    }

    public boolean publishPaymentSucceeded(PaymentSucceededEvent event) {
        if (!mqEnabled) {
            return false;
        }
        RocketMQTemplate rocketMQTemplate = rocketMQTemplateProvider.getIfAvailable();
        if (rocketMQTemplate == null) {
            LOG.warn("RocketMQ未初始化，跳过支付成功事件发送, orderNo: {}", event.getOrderNo());
            return false;
        }
        try {
            rocketMQTemplate.convertAndSend(paymentSucceededTopic, event);
            LOG.info("已发送支付成功MQ事件, topic: {}, orderNo: {}, paymentNo: {}", paymentSucceededTopic, event.getOrderNo(), event.getPaymentNo());
            return true;
        } catch (Exception e) {
            LOG.warn("支付成功MQ事件发送失败，将回退同步通知, orderNo: {}, error: {}", event.getOrderNo(), e.getMessage());
            return false;
        }
    }
}
