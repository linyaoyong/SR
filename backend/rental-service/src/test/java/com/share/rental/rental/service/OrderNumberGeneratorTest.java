package com.share.rental.rental.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderNumberGeneratorTest {

    @Test
    void next_generatesPrefixedTimestampAndFourDigitSuffix() {
        OrderNumberGenerator generator = new OrderNumberGenerator();

        String orderNo = generator.next();

        assertThat(orderNo).startsWith("SR");
        assertThat(orderNo.length()).isEqualTo(2 + 14 + 4);
        String timestamp = orderNo.substring(2, 16);
        assertThat(timestamp).matches("\\d{14}");
        String suffix = orderNo.substring(16);
        assertThat(suffix).matches("\\d{4}");
        int suffixValue = Integer.parseInt(suffix);
        assertThat(suffixValue).isBetween(1000, 9999);
    }

    @Test
    void next_generatesUniqueValues() {
        OrderNumberGenerator generator = new OrderNumberGenerator();

        String first = generator.next();
        String second = generator.next();

        assertThat(first).isNotEqualTo(second);
    }
}
