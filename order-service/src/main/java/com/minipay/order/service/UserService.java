package com.minipay.order.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minipay.common.exception.MinipayException;
import com.minipay.common.exception.MinipayExceptionEnum;
import com.minipay.common.req.UserLoginReq;
import com.minipay.common.resp.UserLoginResp;
import com.minipay.common.util.JwtUtil;
import com.minipay.order.model.User;
import com.minipay.order.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserMapper userMapper;

    /**
     * 用户登录
     * @param req 登录请求
     * @return 登录响应
     */
    public UserLoginResp login(UserLoginReq req) {
        LOG.info("用户登录, req: {}", req);
        String mobile = req.getMobile();
        String code = req.getCode();
        User UserDB = selectByMobile(mobile);
        if (ObjectUtil.isNull(UserDB)) {
//            throw new MinipayException(MinipayExceptionEnum.USER_MOBILE_NOT_EXIST);
            UserDB = new User();
            UserDB.setMobile(mobile);
            userMapper.insert(UserDB);  // 自动创建用户
            LOG.info("新用户已创建, mobile: {}", mobile);
        }
        if (!"8888".equals(code)) {
            throw new MinipayException(MinipayExceptionEnum.USER_MOBILE_CODE_ERROR);
        }
        UserLoginResp userLoginResp = BeanUtil.copyProperties(UserDB, UserLoginResp.class);
        String token = JwtUtil.createToken(userLoginResp.getId());
        userLoginResp.setToken(token);
        return userLoginResp;
    }

    /**
     * 根据手机号查询用户
     * @param mobile 手机号
     * @return 用户信息
     */
    private User selectByMobile(String mobile) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getMobile, mobile);
        return userMapper.selectOne(wrapper);
    }
}