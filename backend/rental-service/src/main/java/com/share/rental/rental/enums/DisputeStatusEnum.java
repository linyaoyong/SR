package com.share.rental.rental.enums;

public enum DisputeStatusEnum {
    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    RESOLVED(2, "已裁定");

    private final int code;
    private final String message;

    DisputeStatusEnum(int code, String message) {
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
