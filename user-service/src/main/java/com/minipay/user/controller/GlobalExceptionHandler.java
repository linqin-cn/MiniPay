package com.minipay.user.controller;

import com.minipay.common.resp.CommonResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
/**
 * 全局异常处理
 * RestControllerAdvice--拦截项目中所有  @RestController 接口抛出的异常，统一格式化返回体
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理非法参数异常--只捕获代码中手动抛出的 IllegalArgumentException（非法参数异常）
     * @param e 异常对象
     * @return 响应结果
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public CommonResp<Void> handleIllegalArgument(IllegalArgumentException e) {
        return new CommonResp<>(400, e.getMessage(), null, false);
    }
    /**
     * 处理重复键异常
     * @param e 异常对象
     * @return 响应结果
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public CommonResp<Void> handleDuplicateKey(DuplicateKeyException e) {
        return new CommonResp<>(400, "用户名已存在", null, false);
    }
    /**
     * 处理数据访问异常
     * @param e 异常对象
     * @return 响应结果
     */
    @ExceptionHandler(DataAccessException.class)
    public CommonResp<Void> handleDataAccess(DataAccessException e) {
        LOG.error("数据库访问异常", e);
        return new CommonResp<>(500, "数据库访问异常，请确认远程数据库已创建并导入初始化SQL", null, false);
    }

    /**
     * 处理其他异常
     * @param e 异常对象
     * @return 响应结果
     */
    @ExceptionHandler(Exception.class)
    public CommonResp<Void> handleException(Exception e) {
        LOG.error("用户服务异常", e);
        return new CommonResp<>(500, "用户服务异常，请查看后端控制台日志", null, false);
    }
}
