package com.minipay.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class GatewayApplication {

    private static final Logger LOG = LoggerFactory.getLogger(GatewayApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(GatewayApplication.class); // 构造Spring启动实例
        Environment env = app.run(args).getEnvironment();// 获取启动环境
        LOG.info("启动成功！！");
        LOG.info("网关地址：\thttp://127.0.0.1:{}",env.getProperty("server.port"));
    }
}
