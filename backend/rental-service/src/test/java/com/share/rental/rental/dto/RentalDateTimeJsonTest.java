package com.share.rental.rental.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RentalDateTimeJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void applicationRequestAcceptsFrontendDateTimeFormat() throws Exception {
        String json = """
                {
                  "itemId": 1,
                  "quantity": 1,
                  "rentStartTime": "2026-06-29 21:41:17",
                  "rentEndTime": "2026-06-30 21:41:17",
                  "deliveryType": 1,
                  "meetupTime": "2026-06-29 22:00:00"
                }
                """;

        RentalApplicationCreateRequest request = objectMapper.readValue(
                json,
                RentalApplicationCreateRequest.class
        );

        assertThat(request.getRentStartTime()).isEqualTo(LocalDateTime.of(2026, 6, 29, 21, 41, 17));
        assertThat(request.getRentEndTime()).isEqualTo(LocalDateTime.of(2026, 6, 30, 21, 41, 17));
        assertThat(request.getMeetupTime()).isEqualTo(LocalDateTime.of(2026, 6, 29, 22, 0, 0));
    }

    @Test
    void proposalRequestAcceptsFrontendDateTimeFormat() throws Exception {
        String json = """
                {
                  "quantity": 2,
                  "rentStartTime": "2026-06-29 21:41:17",
                  "rentEndTime": "2026-06-30 21:41:17",
                  "meetupTime": "2026-06-29 22:00:00"
                }
                """;

        RentalProposalUpdateRequest request = objectMapper.readValue(
                json,
                RentalProposalUpdateRequest.class
        );

        assertThat(request.getRentStartTime()).isEqualTo(LocalDateTime.of(2026, 6, 29, 21, 41, 17));
        assertThat(request.getRentEndTime()).isEqualTo(LocalDateTime.of(2026, 6, 30, 21, 41, 17));
        assertThat(request.getMeetupTime()).isEqualTo(LocalDateTime.of(2026, 6, 29, 22, 0, 0));
    }
}
