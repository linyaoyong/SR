package com.share.rental.wallet.enums;

public enum WalletTransactionTypeEnum {
    RECHARGE(1, "充值"),
    PAY_RENT(2, "支付租金"),
    FREEZE_DEPOSIT(3, "冻结押金"),
    RELEASE_DEPOSIT(4, "释放押金"),
    RENT_INCOME(5, "租金收入"),
    CANCEL_REFUND(6, "取消退款"),
    OVERDUE_EXPENSE(7, "逾期支出"),
    OVERDUE_INCOME(8, "逾期收入"),
    DEPOSIT_DEDUCTION(9, "押金扣除");

    private final int code;
    private final String message;

    WalletTransactionTypeEnum(int code, String message) {
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
