package com.share.rental.auth.enums;

public enum UserStatusEnum {
    NORMAL(0, "正常"),
    BANNED(1, "已封禁");

    private final int code;
    private final String message;

    UserStatusEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
