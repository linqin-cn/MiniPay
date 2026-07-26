package com.minipay.gateway.util;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.crypto.GlobalBouncyCastleProvider;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类，负责生成和验证JWT令牌
 */
public class JwtUtil {
    private static final Logger LOG = LoggerFactory.getLogger(JwtUtil.class);
    private static final String KEY = "yuanshenniubi";

    // 创建token
    public static String createToken(Long userId) {
        DateTime nowTime = new DateTime();
        DateTime expTime = nowTime.offsetNew(DateField.HOUR, 24);
        LOG.info("开始生成JWT令牌，用户id{}", userId);
        Map<String, Object> paymap = new HashMap<>();
        paymap.put("userId", userId);
        paymap.put("createTime", nowTime);
        paymap.put(JWTPayload.EXPIRES_AT, expTime);
        paymap.put(JWTPayload.NOT_BEFORE, nowTime);
        String token = JWTUtil.createToken(paymap, KEY.getBytes());
        LOG.info("成功生成JWT token：{}", token);
        return token;
    }

    // 校验token是否有效
    public static boolean validate(String token) {
        try {
            LOG.info("开始JWT token校验，token：{}", token);
            GlobalBouncyCastleProvider.setUseBouncyCastle(false);
            JWT jwt = JWTUtil.parseToken(token).setKey(KEY.getBytes());
            boolean validate = jwt.validate(0);
            LOG.info("JWT token校验结果：{}", validate);
            return validate;
        } catch (Exception e) {
            LOG.error("JWT token校验异常", e);
            return false;
        }
    }

    // 从token中获取userid
    public static Long getUserId(String token) {
        try {
            // 禁用BC加密
            GlobalBouncyCastleProvider.setUseBouncyCastle(false);
            // 解析JWT令牌
            JWT jwt = JWTUtil.parseToken(token).setKey(KEY.getBytes());
            // 读取 JWT 载体里存放的用户编号数据。
            Object userId = jwt.getPayload("userId");
            // 转换为Long类型
            return userId == null ? null : Long.valueOf(String.valueOf(userId));
        } catch (Exception e) {
            LOG.error("解析JWT用户ID异常", e);
            return null;
        }
    }
}
