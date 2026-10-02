package com.share.rental.admin.enums;

public enum TargetTypeEnum {
    USER("USER", "用户"),
    ITEM("ITEM", "物品"),
    ORDER("ORDER", "订单"),
    DISPUTE("DISPUTE", "异议");

    private final String code;
    private final String message;

    TargetTypeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
