package com.share.rental.rental.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class OrderNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public String next() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int suffix = RANDOM.nextInt(9000) + 1000;
        return "SR" + timestamp + suffix;
    }
}
