package com.share.rental.item.enums;

public enum ItemStatusEnum {
    PENDING(0, "待上架"),
    LISTED(1, "已上架"),
    OFF_SHELF(2, "已下架"),
    FORCE_OFF_SHELF(3, "强制下架");

    private final int code;
    private final String message;

    ItemStatusEnum(int code, String message) {
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
