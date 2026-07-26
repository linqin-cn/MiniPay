package com.minipay.payment.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.minipay.payment.config.AlipayConfig;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AlipayService {
    private static final Logger LOG = LoggerFactory.getLogger(AlipayService.class);

    @Resource
    private AlipayConfig alipayConfig;

    private AlipayClient alipayClient;

    /**
     * 获取支付宝客户端
     * @return AlipayClient
     */
    private AlipayClient getAlipayClient() {
        // 懒加载单例：只有第一次调用该方法时才创建对象；已经实例化过直接跳过创建逻辑，节省资源开销。
        if (alipayClient == null) {
            // 获取AlipayConfig的网关URL--GatewayUrl
            String gatewayUrl = alipayConfig.getGatewayUrl().trim();
            if (!gatewayUrl.startsWith("https://")) {
                // 支付宝官方接口强制要求 HTTPS 传输，http 无法正常调用接口 ，所以替换为https://
                gatewayUrl = "https://" + gatewayUrl.replaceFirst("http://", "");
            }
            //  创建alipayClient对象
            alipayClient = new DefaultAlipayClient(
                gatewayUrl,
                alipayConfig.getAppId(),
                alipayConfig.getPrivateKey(),
                alipayConfig.getFormat(),
                alipayConfig.getCharset(),
                alipayConfig.getAlipayPublicKey(),
                alipayConfig.getSignType()
            );
        }
        return alipayClient;
    }

    /**
     * 创建支付
     * @param outTradeNo 外部订单号
     * @param subject 商品名称
     * @param totalAmount 支付金额
     * @return Map<String, String>
     */
    public Map<String, String> createPayment(String outTradeNo, String subject, String totalAmount) {
        LOG.info("调用支付宝创建支付, 订单号: {}, 金额: {}", outTradeNo, totalAmount);
        Map<String, String> result = new HashMap<>();

        // 最多重试2次
        for (int i = 0; i < 2; i++) {
            try {
                // 构建支付宝请求对象
                AlipayTradePrecreateRequest request = getAlipayTradePrecreateRequest(outTradeNo, subject, totalAmount);

                LOG.info("发送支付宝API请求 (第{}次)...", i + 1);
                // 发送加密签名请求，调用支付宝官方接口
                AlipayTradePrecreateResponse response = getAlipayClient().execute(request);
                LOG.info("支付宝API响应: code={}, sub_code={}", response.getCode(), response.getSubCode());

                if (response.isSuccess()) {
                    LOG.info("支付宝预下单成功, qr_code: {}", response.getQrCode());
                    result.put("code", "1");
                    result.put("qr_code", response.getQrCode());
                    result.put("out_trade_no", response.getOutTradeNo());
                    return result;
                } else {
                    LOG.error("支付宝预下单失败: {} - {}", response.getCode(), response.getMsg());
                    result.put("code", "0");
                    result.put("msg", response.getSubMsg() != null ? response.getSubMsg() : response.getMsg());
                    return result;
                }
            } catch (AlipayApiException e) {
                LOG.error("调用支付宝API异常 (第{}次): {}", i + 1, e.getMessage());
                if (i < 1) {
                    LOG.info("等待2秒后重试...");
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                } else {
                    result.put("code", "0");
                    result.put("msg", "网络超时，请稍后重试");
                }
            }
        }
        return result;
    }

    /**
     * 封装构建支付宝当面付预下单请求对象的通用方法
     */
    @NotNull
    private AlipayTradePrecreateRequest getAlipayTradePrecreateRequest(String outTradeNo, String subject, String totalAmount) {
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        // 异步回调地址，支付宝在用户扫码付款完成后，会向该地址发送支付结果通知
        request.setNotifyUrl(alipayConfig.getNotifyUrl());
        // 用户扫码付款完成后，浏览器跳转的页面地址
        request.setReturnUrl(alipayConfig.getReturnUrl());
        /*bizContent 是支付宝规定的业务入参，JSON 字符串格式：
        out_trade_no：我方自定义支付单号（唯一，对账依据）
        total_amount：支付金额（单位：元，保留两位小数）
        subject：订单名称 / 商品标题，支付宝账单展示的名称*/
        request.setBizContent(String.format(
            "{\"out_trade_no\":\"%s\",\"total_amount\":\"%s\",\"subject\":\"%s\"}",
                outTradeNo, totalAmount, subject
        ));
        return request;
    }

    /**
     * 验证支付宝回调通知--支付宝的签名验证
     * @param params 回调参数
     * @return boolean
     */
    public boolean verifyNotify(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCheckV1(
                params,
                alipayConfig.getAlipayPublicKey(),
                alipayConfig.getCharset(),
                alipayConfig.getSignType()
            );
        } catch (AlipayApiException e) {
            LOG.error("验证支付宝签名失败", e);
            return false;
        }
    }
}
