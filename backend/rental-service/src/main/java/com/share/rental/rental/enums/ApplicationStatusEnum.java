package com.share.rental.rental.enums;

public enum ApplicationStatusEnum {
    NEGOTIATING(0, "待协商"),
    CONFIRMED(1, "已确认"),
    CONVERTED(2, "已转单"),
    CANCELLED(3, "已取消");

    private final int code;
    private final String message;

    ApplicationStatusEnum(int code, String message) {
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
