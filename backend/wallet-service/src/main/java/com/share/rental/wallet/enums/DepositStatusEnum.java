package com.share.rental.wallet.enums;

public enum DepositStatusEnum {
    FROZEN(0, "已冻结"),
    RELEASED(1, "已释放"),
    DEDUCTED(2, "已扣除"),
    CANCELLED(3, "已取消"),
    PARTIAL_DEDUCTED_RELEASED(4, "部分扣除后已释放");

    private final int code;
    private final String message;

    DepositStatusEnum(int code, String message) {
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
