package com.share.rental.message.enums;

public enum MessageTypeEnum {
    TEXT(1, "文本"),
    IMAGE(2, "图片"),
    CARD(3, "卡片"),
    SYSTEM_NOTIFICATION(4, "系统通知"),
    AUDIT_NOTIFICATION(5, "审核通知");

    private final int code;
    private final String message;

    MessageTypeEnum(int code, String message) {
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
