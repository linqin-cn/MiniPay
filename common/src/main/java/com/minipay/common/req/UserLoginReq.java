package com.minipay.common.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户登录请求参数
 */
@Data
public class UserLoginReq {
    @NotBlank(message = "【手机号】不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号码格式错误")
    private String mobile;

    @NotBlank(message = "【短信验证码】不能为空")
    private String code;
}
