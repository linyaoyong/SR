package com.share.rental.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    void rentalErrorCodesExposeExpectedCodesAndMessages() {
        assertThat(ErrorCode.RENTAL_APPLICATION_NOT_FOUND.code()).isEqualTo(40300);
        assertThat(ErrorCode.RENTAL_APPLICATION_NOT_FOUND.message()).isEqualTo("租借申请不存在");
        assertThat(ErrorCode.RENTAL_APPLICATION_PERMISSION_DENIED.code()).isEqualTo(40301);
        assertThat(ErrorCode.RENTAL_APPLICATION_PERMISSION_DENIED.message()).isEqualTo("无权操作该租借申请");
        assertThat(ErrorCode.RENTAL_APPLICATION_STATUS_INVALID.code()).isEqualTo(40302);
        assertThat(ErrorCode.RENTAL_APPLICATION_STATUS_INVALID.message()).isEqualTo("租借申请状态不允许该操作");
        assertThat(ErrorCode.RENTAL_PROPOSAL_NOT_FOUND.code()).isEqualTo(40303);
        assertThat(ErrorCode.RENTAL_PROPOSAL_NOT_FOUND.message()).isEqualTo("协商版本不存在");
        assertThat(ErrorCode.RENTAL_TIME_INVALID.code()).isEqualTo(40304);
        assertThat(ErrorCode.RENTAL_TIME_INVALID.message()).isEqualTo("租借时间不合法");
        assertThat(ErrorCode.RENTAL_TIME_STOCK_NOT_ENOUGH.code()).isEqualTo(40305);
        assertThat(ErrorCode.RENTAL_TIME_STOCK_NOT_ENOUGH.message()).isEqualTo("时间段库存不足");
        assertThat(ErrorCode.RENTAL_ORDER_NOT_FOUND.code()).isEqualTo(40306);
        assertThat(ErrorCode.RENTAL_ORDER_NOT_FOUND.message()).isEqualTo("租借订单不存在");
        assertThat(ErrorCode.RENTAL_ORDER_STATUS_INVALID.code()).isEqualTo(40307);
        assertThat(ErrorCode.RENTAL_ORDER_STATUS_INVALID.message()).isEqualTo("订单状态不允许该操作");
        assertThat(ErrorCode.RENTAL_OWNER_CANNOT_RENT_OWN_ITEM.code()).isEqualTo(40308);
        assertThat(ErrorCode.RENTAL_OWNER_CANNOT_RENT_OWN_ITEM.message()).isEqualTo("不能租借自己的物品");
        assertThat(ErrorCode.RENTAL_ITEM_UNAVAILABLE.code()).isEqualTo(40309);
        assertThat(ErrorCode.RENTAL_ITEM_UNAVAILABLE.message()).isEqualTo("物品当前不可租借");
    }
}
