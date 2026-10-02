package com.share.rental.common.enums;

public enum CommonAuditStatus {
    PENDING(0, "未审核"),
    APPROVED(1, "审核通过"),
    RECTIFY(2, "要求整改");

    private final int code;
    private final String message;

    CommonAuditStatus(int code, String message) {
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
