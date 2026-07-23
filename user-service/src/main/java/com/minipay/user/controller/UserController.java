package com.minipay.user.controller;

import com.minipay.common.resp.CommonResp;
import com.minipay.user.dto.UserAddressReq;
import com.minipay.user.dto.UserLoginReq;
import com.minipay.user.dto.UserRegisterReq;
import com.minipay.user.model.User;
import com.minipay.user.model.UserAddress;
import com.minipay.user.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Resource
    private UserService userService;

    @PostMapping("/register")
    public CommonResp<User> register(@RequestBody UserRegisterReq req) { return new CommonResp<>(200, "TODO", userService.register(req), true); }

    @PostMapping("/login")
    public CommonResp<Object> login(@RequestBody UserLoginReq req) { return new CommonResp<>(200, "TODO", userService.login(req), true); }

    @GetMapping("/me")
    public CommonResp<User> me() { return new CommonResp<>(200, "TODO", userService.getCurrentUser(), true); }

    @GetMapping("/addresses")
    public CommonResp<List<UserAddress>> listAddresses() { return new CommonResp<>(200, "TODO", userService.listAddresses(), true); }

    @PostMapping("/addresses")
    public CommonResp<UserAddress> createAddress(@RequestBody UserAddressReq req) { return new CommonResp<>(200, "TODO", userService.createAddress(req), true); }

    @PutMapping("/addresses/{id}")
    public CommonResp<UserAddress> updateAddress(@PathVariable Long id, @RequestBody UserAddressReq req) { return new CommonResp<>(200, "TODO", userService.updateAddress(id, req), true); }

    @DeleteMapping("/addresses/{id}")
    public CommonResp<Void> deleteAddress(@PathVariable Long id) { userService.deleteAddress(id); return new CommonResp<>(200, "TODO", null, true); }

    @PutMapping("/addresses/{id}/default")
    public CommonResp<UserAddress> setDefaultAddress(@PathVariable Long id) { return new CommonResp<>(200, "TODO", userService.setDefaultAddress(id), true); }

    @GetMapping("/health")
    public CommonResp<String> health() { return new CommonResp<>(200, "success", "user-service is running", true); }
}
