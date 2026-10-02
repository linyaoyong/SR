package com.share.rental.admin.enums;

public enum OperationTypeEnum {
    AUDIT_USER("AUDIT_USER", "用户资料审核"),
    AUDIT_ITEM("AUDIT_ITEM", "物品审核"),
    BAN_USER("BAN_USER", "封禁用户"),
    UNBAN_USER("UNBAN_USER", "解封用户"),
    FORCE_OFF_SHELF("FORCE_OFF_SHELF", "强制下架"),
    RESOLVE_DISPUTE("RESOLVE_DISPUTE", "处理异议");

    private final String code;
    private final String message;

    OperationTypeEnum(String code, String message) {
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
