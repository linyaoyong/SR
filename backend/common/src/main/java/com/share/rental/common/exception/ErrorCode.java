package com.share.rental.common.exception;

public enum ErrorCode {
    SUCCESS(0, "success"),
    BAD_REQUEST(40000, "参数错误"),
    UNAUTHORIZED(40001, "未登录或登录已过期"),
    FORBIDDEN(40002, "无权限"),
    INTERNAL_FORBIDDEN(40003, "禁止外部访问内部接口"),
    VALIDATION_ERROR(40004, "参数校验失败"),
    AUTH_USER_NOT_FOUND(40100, "用户不存在"),
    AUTH_BAD_CREDENTIALS(40101, "用户名或密码错误"),
    AUTH_ACCOUNT_BANNED(40102, "账户已封禁"),
    AUTH_USERNAME_EXISTS(40103, "用户名已存在"),
    AUTH_USERNAME_INVALID(40104, "用户名格式非法"),
    AUTH_NOT_ADMIN(40105, "该账号不是管理员"),
    AUTH_BLACKLIST_EXISTS(40106, "已拉黑该用户"),
    AUTH_BLACKLIST_NOT_FOUND(40107, "拉黑记录不存在"),
    AUTH_CANNOT_BLACKLIST_SELF(40108, "不能拉黑自己"),
    AUTH_REFRESH_TOKEN_INVALID(40109, "刷新令牌无效"),
    WALLET_NOT_FOUND(40400, "钱包账户不存在"),
    WALLET_INSUFFICIENT_BALANCE(40401, "钱包余额不足"),
    WALLET_IN_ARREARS(40402, "钱包欠费"),
    WALLET_RECHARGE_AMOUNT_INVALID(40403, "充值金额非法"),
    WALLET_DEPOSIT_ALREADY_FROZEN(40404, "押金已冻结"),
    WALLET_DEPOSIT_NOT_FROZEN(40405, "押金未冻结"),
    WALLET_DEPOSIT_ALREADY_RELEASED(40406, "押金已释放"),
    WALLET_FREEZE_FAILED(40407, "押金冻结失败"),
    WALLET_RELEASE_FAILED(40408, "押金释放失败"),
    WALLET_SETTLEMENT_ALREADY_EXISTS(40409, "订单结算记录已存在"),
    WALLET_SETTLEMENT_NOT_FOUND(40410, "订单结算记录不存在"),
    WALLET_OVERDUE_CALCULATION_FAILED(40411, "逾期费用计算失败"),
    WALLET_UPDATE_CONFLICT(40412, "钱包账户更新冲突，请重试"),
    ITEM_NOT_FOUND(40200, "物品不存在"),
    ITEM_OFF_SHELF(40201, "物品已下架"),
    ITEM_AUDIT_RECTIFY(40202, "物品要求整改"),
    ITEM_IMAGE_TOO_MANY(40203, "物品图片过多"),
    ITEM_OWNER_REQUIRED(40204, "只有物品拥有者可以操作"),
    ITEM_CATEGORY_NOT_FOUND(40205, "分类不存在"),
    ITEM_PUBLISH_FORBIDDEN(40206, "当前账号不能发布物品"),
    FILE_TYPE_NOT_SUPPORTED(40207, "图片类型不支持"),
    FILE_TOO_LARGE(40208, "图片超过大小限制"),
    FILE_UPLOAD_FAILED(40209, "图片上传失败"),
    RENTAL_APPLICATION_NOT_FOUND(40300, "租借申请不存在"),
    RENTAL_APPLICATION_PERMISSION_DENIED(40301, "无权操作该租借申请"),
    RENTAL_APPLICATION_STATUS_INVALID(40302, "租借申请状态不允许该操作"),
    RENTAL_PROPOSAL_NOT_FOUND(40303, "协商版本不存在"),
    RENTAL_TIME_INVALID(40304, "租借时间不合法"),
    RENTAL_TIME_STOCK_NOT_ENOUGH(40305, "时间段库存不足"),
    RENTAL_ORDER_NOT_FOUND(40306, "租借订单不存在"),
    RENTAL_ORDER_STATUS_INVALID(40307, "订单状态不允许该操作"),
    RENTAL_OWNER_CANNOT_RENT_OWN_ITEM(40308, "不能租借自己的物品"),
    RENTAL_ITEM_UNAVAILABLE(40309, "物品当前不可租借"),
    REVIEW_ALREADY_EXISTS(40310, "该订单已评价过"),
    REVIEW_NOT_FOUND(40311, "评价不存在"),
    REVIEW_PERMISSION_DENIED(40312, "无权操作此评价"),
    REVIEW_ORDER_NOT_COMPLETED(40313, "订单未完成，不可评价"),
    DISPUTE_ALREADY_EXISTS(40314, "该订单已提交异议"),
    DISPUTE_NOT_FOUND(40315, "异议不存在"),
    DISPUTE_PERMISSION_DENIED(40316, "无权操作此异议"),
    DISPUTE_STATUS_INVALID(40317, "异议状态不允许此操作"),
    ADMIN_NOT_ALLOWED(40600, "管理员权限不足"),
    ADMIN_AUDIT_TARGET_NOT_FOUND(40601, "审核对象不存在"),
    REQUEST_TOO_FREQUENT(42900, "请求过于频繁"),
    SERVICE_BUSY(42901, "服务繁忙"),
    SYSTEM_ERROR(50000, "系统异常"),
    REMOTE_CALL_FAILED(50001, "远程服务调用失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
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
