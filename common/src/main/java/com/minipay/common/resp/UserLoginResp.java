package com.minipay.common.resp;

import lombok.Data;

/**
 * 用户登录响应结果
 */
@Data
public class UserLoginResp {
    // 用户ID
    private Long id;

    // 用户手机号
    private String mobile;

    // 用户token
    private String token;

    // 用户名--（唯一、用于系统身份核验）
    private String username;

    // 用户昵称--可重复
    private String nickname;

    // 用户角色
    private String role;
}
