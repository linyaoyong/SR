package com.share.rental.rental.enums;

public enum DeliveryTypeEnum {
    MEETUP(0, "面交"),
    EXPRESS(1, "快递");

    private final int code;
    private final String message;

    DeliveryTypeEnum(int code, String message) {
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
