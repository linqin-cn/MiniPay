package com.minipay.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.minipay.common.resp.UserLoginResp;
import com.minipay.common.util.JwtUtil;
import com.minipay.user.dto.UserAddressReq;
import com.minipay.user.dto.UserLoginReq;
import com.minipay.user.dto.UserRegisterReq;
import com.minipay.user.mapper.UserAddressMapper;
import com.minipay.user.mapper.UserMapper;
import com.minipay.user.model.User;
import com.minipay.user.model.UserAddress;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {
    @Resource
    private UserMapper userMapper;

    @Resource
    private UserAddressMapper userAddressMapper;

    @Resource
    private HttpServletRequest request;

    /**
     * 用户注册
     * @param req 用户注册请求参数
     * @return 用户实体
     */
    public User register(UserRegisterReq req) {
        if (req == null || isBlank(req.getUsername()) || isBlank(req.getPassword())) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, req.getUsername());
        wrapper.eq(User::getRole, normalizeRole(req.getRole()));
        if (userMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(req.getPassword());
        user.setNickname(isBlank(req.getNickname()) ? req.getUsername() : req.getNickname());
        user.setPhone(req.getPhone());
        user.setRole(normalizeRole(req.getRole()));
        user.setStatus("ACTIVE");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return user;
    }

    /**
     * 用户登录
     * @param req 用户登录请求参数
     * @return 用户登录响应结果
     */
    public UserLoginResp login(UserLoginReq req) {
        if (req == null || isBlank(req.getPassword())) {
            throw new IllegalArgumentException("账号和密码不能为空");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (!isBlank(req.getUsername())) {
            wrapper.eq(User::getUsername, req.getUsername());
        } else if (!isBlank(req.getPhone())) {
            wrapper.eq(User::getPhone, req.getPhone());
        } else {
            throw new IllegalArgumentException("账号和密码不能为空");
        }
        wrapper.eq(User::getPassword, req.getPassword())
                .in(User::getStatus, "ACTIVE", "NORMAL");
        if (!isBlank(req.getRole())) {
            wrapper.eq(User::getRole, normalizeRole(req.getRole()));
        }
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        UserLoginResp resp = new UserLoginResp();
        resp.setId(user.getId());
        resp.setMobile(user.getPhone());
        resp.setToken(JwtUtil.createToken(user.getId()));
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setRole(user.getRole());
        return resp;
    }

    /**
     * 获取当前用户
     * @return 用户实体
     */
    @Cacheable(cacheNames = "user:current", key = "#root.target.getCurrentUserId()", unless = "#result == null")
    public User getCurrentUser() {
        return userMapper.selectById(getCurrentUserId());
    }

    /**
     * 列出当前用户的所有地址
     * @return 地址列表
     */
    @Cacheable(cacheNames = "user:addresses", key = "#root.target.getCurrentUserId()")
    public List<UserAddress> listAddresses() {
        LambdaQueryWrapper<UserAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAddress::getUserId, getCurrentUserId()).orderByDesc(UserAddress::getIsDefault).orderByDesc(UserAddress::getUpdatedAt);
        return userAddressMapper.selectList(wrapper);
    }

    /**
     * 创建用户地址
     * @param req 用户地址请求参数
     * @return 用户地址实体
     */
    @Transactional
    @CacheEvict(cacheNames = "user:addresses", key = "#root.target.getCurrentUserId()")
    public UserAddress createAddress(UserAddressReq req) {
        UserAddress address = new UserAddress();
        copyAddress(req, address);
        address.setUserId(getCurrentUserId());
        LocalDateTime now = LocalDateTime.now();
        address.setCreatedAt(now);
        address.setUpdatedAt(now);
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            clearDefaultAddress();
        }
        userAddressMapper.insert(address);
        return address;
    }

    /**
     * 更新用户地址
     * @param id 用户地址ID
     * @param req 用户地址请求参数
     * @return 用户地址实体
     */
    @Transactional
    @CacheEvict(cacheNames = "user:addresses", key = "#root.target.getCurrentUserId()")
    public UserAddress updateAddress(Long id, UserAddressReq req) {
        UserAddress address = userAddressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(getCurrentUserId())) {
            throw new IllegalArgumentException("收货地址不存在");
        }
        copyAddress(req, address);
        address.setUpdatedAt(LocalDateTime.now());
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            clearDefaultAddress();
        }
        userAddressMapper.updateById(address);
        return address;
    }

    /**
     * 删除用户地址
     * @param id 用户地址ID
     */
    @CacheEvict(cacheNames = "user:addresses", key = "#root.target.getCurrentUserId()")
    public void deleteAddress(Long id) {
        LambdaQueryWrapper<UserAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAddress::getId, id).eq(UserAddress::getUserId, getCurrentUserId());
        userAddressMapper.delete(wrapper);
    }

    /**
     * 设置默认地址
     * @param id 用户地址ID
     * @return 用户地址实体
     */
    @Transactional
    @CacheEvict(cacheNames = "user:addresses", key = "#root.target.getCurrentUserId()")
    public UserAddress setDefaultAddress(Long id) {
        // 获取地址
        UserAddress address = userAddressMapper.selectById(id);
        // 地址归属的用户 ID ≠ 当前登录用户 ID（越权访问，别人的地址你不能修改）
        if (address == null || !address.getUserId().equals(getCurrentUserId())) {
            throw new IllegalArgumentException("收货地址不存在");
        }
        // 清除当前用户的默认地址
        clearDefaultAddress();
        //  将本次选中地址标记为默认
        address.setIsDefault(true);
        address.setUpdatedAt(LocalDateTime.now());
        userAddressMapper.updateById(address);
        return address;
    }

    /**
     * 复制地址信息
     * @param req 请求参数
     * @param address 地址实体
     */
    private void copyAddress(UserAddressReq req, UserAddress address) {
        if (req == null) {
            throw new IllegalArgumentException("地址信息不能为空");
        }
        address.setReceiverName(req.getReceiverName());
        address.setReceiverPhone(req.getReceiverPhone());
        address.setProvince(req.getProvince());
        address.setCity(req.getCity());
        address.setDistrict(req.getDistrict());
        address.setDetailAddress(req.getDetailAddress());
        address.setIsDefault(Boolean.TRUE.equals(req.getIsDefault()));
    }

    /**
     * 清除默认地址
     */
    private void clearDefaultAddress() {
        LambdaUpdateWrapper<UserAddress> wrapper = new LambdaUpdateWrapper<>();
        // 当前登录用户的所有地址，执行set：把这些地址的 is_default 字段统一改为 false（取消默认）
        wrapper.eq(UserAddress::getUserId, getCurrentUserId()).set(UserAddress::getIsDefault, false);
        userAddressMapper.update(null, wrapper);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 标准化用户角色
     * @param role 用户角色
     * @return 标准化后的用户角色
     */
    private String normalizeRole(String role) {
        if ("MERCHANT".equalsIgnoreCase(role)) {
            return "MERCHANT";
        }
        return "BUYER";
    }

    /**
     * 获取当前用户ID
     * @return 用户ID
     */
    public Long getCurrentUserId() {
        String token = request.getHeader("token");
        Long userId = JwtUtil.getUserId(token);
        if (userId == null) {
            throw new IllegalArgumentException("登录已失效，请重新登录");
        }
        return userId;
    }
}
