package com.share.rental.common.exception;

import com.share.rental.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void validationExceptionReturnsStableMessage() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(
                new ConstraintViolationException("internal dto rejected value", Set.of())
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.VALIDATION_ERROR.code());
        assertThat(response.getBody().message()).isEqualTo(ErrorCode.VALIDATION_ERROR.message());
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    void unreadableJsonReturnsValidationErrorInsteadOfSystemError() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnreadableJsonException(
                new HttpMessageNotReadableException("invalid json")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.VALIDATION_ERROR.code());
        assertThat(response.getBody().message()).isEqualTo(ErrorCode.VALIDATION_ERROR.message());
        assertThat(response.getBody().data()).isNull();
    }
}
