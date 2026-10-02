package com.share.rental.rental.enums;

public enum TimeLockStatusEnum {
    OCCUPIED(0, "占用"),
    RELEASED(1, "已释放");

    private final int code;
    private final String message;

    TimeLockStatusEnum(int code, String message) {
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
