package com.share.rental.message.enums;

public enum CardTypeEnum {
    APPLICATION(1, "申请卡片"),
    NEGOTIATION_UPDATE(2, "协商修改卡片"),
    ORDER(3, "订单卡片"),
    REVIEW_REMINDER(4, "评价提醒卡片"),
    DISPUTE(5, "异议卡片");

    private final int code;
    private final String message;

    CardTypeEnum(int code, String message) {
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
