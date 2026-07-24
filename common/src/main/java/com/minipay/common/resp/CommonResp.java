package com.minipay.common.resp;

public class CommonResp<T>{
    // 响应码
    private int code;
    // 提示信息
    private String message;
    // 响应数据
    private T data;
    // 业务成功或失败
    private boolean success;


    public CommonResp() {
    }

    public CommonResp(int code, String message, T data, boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
