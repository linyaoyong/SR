package com.share.rental.item.enums;

public enum PriceTypeEnum {
    FREE(0, "免费"),
    DAILY(1, "按天计价");

    private final int code;
    private final String message;

    PriceTypeEnum(int code, String message) {
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
