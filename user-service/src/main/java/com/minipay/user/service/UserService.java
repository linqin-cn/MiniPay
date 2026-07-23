package com.minipay.user.service;

import com.minipay.user.dto.UserAddressReq;
import com.minipay.user.dto.UserLoginReq;
import com.minipay.user.dto.UserRegisterReq;
import com.minipay.user.model.User;
import com.minipay.user.model.UserAddress;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class UserService {
    public User register(UserRegisterReq req) { return null; }
    public Object login(UserLoginReq req) { return null; }
    public User getCurrentUser() { return null; }
    public List<UserAddress> listAddresses() { return Collections.emptyList(); }
    public UserAddress createAddress(UserAddressReq req) { return null; }
    public UserAddress updateAddress(Long id, UserAddressReq req) { return null; }
    public void deleteAddress(Long id) { }
    public UserAddress setDefaultAddress(Long id) { return null; }
}
