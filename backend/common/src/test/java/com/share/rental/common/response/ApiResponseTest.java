package com.share.rental.common.response;

import com.share.rental.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void successReturnsSuccessCodeMessageAndData() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertThat(response.code()).isEqualTo(0);
        assertThat(response.message()).isEqualTo("success");
        assertThat(response.data()).isEqualTo("ok");
    }

    @Test
    void errorReturnsCodeMessageAndNullData() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.INTERNAL_FORBIDDEN);

        assertThat(response.code()).isEqualTo(40003);
        assertThat(response.message()).isEqualTo("禁止外部访问内部接口");
        assertThat(response.data()).isNull();
    }
}
