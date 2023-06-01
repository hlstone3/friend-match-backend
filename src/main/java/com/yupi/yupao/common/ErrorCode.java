package com.yupi.yupao.common;

public enum ErrorCode {
    SUCCESS(0, "ok", ""),
    PARAMS_ERROR(40000, "请求参数错误", ""),
    NULL_ERROR(40001, "请求为空", ""),
    NOT_LOGIN(40100, "未登录", ""),
    UNKNOWN_ERROR(40100, "未知错误", ""),
    SYSTEM_ERROR(50000, "系统内部异常", ""),
    NO_AUTH(40101, "无权限", "");

    private final int code;
    private final String message;
    private final String description;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }

    ErrorCode(int code, String message, String description) {
        this.code = code;
        this.message = message;
        this.description = description;
    }

}
