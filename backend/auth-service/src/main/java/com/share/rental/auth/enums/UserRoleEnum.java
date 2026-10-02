package com.share.rental.auth.enums;

public enum UserRoleEnum {
    USER(0, "普通用户"),
    ADMIN(1, "管理员");

    private final int code;
    private final String message;

    UserRoleEnum(int code, String message) {
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
