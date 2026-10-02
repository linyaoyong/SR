package com.share.rental.common.response;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageResultTest {

    @Test
    void ofCalculatesPagesAndKeepsFields() {
        PageResult<String> result = PageResult.of(List.of("a", "b"), 21, 2, 10);

        assertThat(result.records()).containsExactly("a", "b");
        assertThat(result.total()).isEqualTo(21);
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.pages()).isEqualTo(3);
    }

    @Test
    void ofReturnsZeroPagesWhenTotalIsZero() {
        PageResult<String> result = PageResult.of(List.of(), 0, 1, 20);

        assertThat(result.records()).isEmpty();
        assertThat(result.total()).isZero();
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.pages()).isZero();
    }

    @Test
    void ofRejectsInvalidPageArguments() {
        assertThatThrownBy(() -> PageResult.of(List.of(), 0, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("page must be greater than or equal to 1");

        assertThatThrownBy(() -> PageResult.of(List.of(), 0, 1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("size must be greater than or equal to 1");

        assertThatThrownBy(() -> PageResult.of(List.of(), -1, 1, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("total must be greater than or equal to 0");

        assertThatThrownBy(() -> PageResult.of(null, 0, 1, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("records must not be null");
    }
}
