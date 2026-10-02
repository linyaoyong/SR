package com.share.rental.rental.enums;

public enum OrderStatusEnum {
    PENDING_PAYMENT(0, "待付款"),
    PAID_PENDING_DELIVERY(1, "已付款待交付"),
    SHIPPED(2, "已发货"),
    RENTING(3, "租借中"),
    PENDING_RETURN_CONFIRM(4, "待归还确认"),
    COMPLETED(5, "已完成"),
    CANCELLED(6, "已取消"),
    DISPUTING(7, "异议中"),
    CLOSED(8, "已关闭");

    private final int code;
    private final String message;

    OrderStatusEnum(int code, String message) {
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
