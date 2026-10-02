package com.share.rental.rental.enums;

public enum ReviewStatusEnum {
    NORMAL(0, "正常"),
    HIDDEN(1, "已隐藏");

    private final int code;
    private final String message;

    ReviewStatusEnum(int code, String message) {
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
