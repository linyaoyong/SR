package com.share.rental.wallet.enums;

public enum SettlementStatusEnum {
    UNSETTLED(0, "未结算"),
    PARTIAL_SETTLED(1, "部分结算"),
    SETTLED(2, "已结算");

    private final int code;
    private final String message;

    SettlementStatusEnum(int code, String message) {
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
