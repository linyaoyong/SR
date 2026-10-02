package com.share.rental.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.rental.admin.client.RentalAdminClient;
import com.share.rental.admin.dto.DisputeResponse;
import com.share.rental.admin.dto.ResolveDisputeRequest;
import com.share.rental.common.exception.GlobalExceptionHandler;
import com.share.rental.common.response.ApiResponse;
import com.share.rental.common.upload.UploadProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminDisputeController.class)
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(UploadProperties.class)
class AdminDisputeControllerTest {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.web.context.WebApplicationContext gatewayFixtureContext;

    @org.junit.jupiter.api.BeforeEach
    void useExplicitGatewayCredentialFixture() {
        mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .webAppContextSetup(gatewayFixtureContext)
                .defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/")
                        .header("X-Internal-Token", "test-only-backend-ingress-token"))
                .build();
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RentalAdminClient rentalAdminClient;

    @Test
    void listDisputes_returns200() throws Exception {
        when(rentalAdminClient.listDisputes(eq(1)))
                .thenReturn(ApiResponse.success(List.of(new DisputeResponse(
                        1L, 100L, 10L, "物品损坏", "用户租用期间刮花",
                        new BigDecimal("50.00"), null, 1, null, null,
                        LocalDateTime.of(2026, 6, 27, 10, 0),
                        LocalDateTime.of(2026, 6, 27, 10, 0)))));

        mvc.perform(get("/api/admin/disputes").param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].orderId").value(100))
                .andExpect(jsonPath("$.data[0].applicantId").value(10))
                .andExpect(jsonPath("$.data[0].status").value(1));
    }

    @Test
    void resolveDispute_returns200() throws Exception {
        when(rentalAdminClient.resolveDispute(eq(1L), any(ResolveDisputeRequest.class)))
                .thenReturn(ApiResponse.success());

        ResolveDisputeRequest req = new ResolveDisputeRequest();
        req.setAdminId(7L);
        req.setAdminRemark("扣除 50 元押金，剩余退还");

        mvc.perform(put("/api/admin/disputes/1/resolve")
                        .header("X-User-Id", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        ArgumentCaptor<ResolveDisputeRequest> captor = ArgumentCaptor.forClass(ResolveDisputeRequest.class);
        verify(rentalAdminClient).resolveDispute(eq(1L), captor.capture());
        assertThat(captor.getValue().getAdminId()).isEqualTo(999L);
        assertThat(captor.getValue().getAdminRemark()).isEqualTo("扣除 50 元押金，剩余退还");
    }

    @Test
    void resolveDispute_bodyAdminIdMissing_usesHeaderAdminId() throws Exception {
        when(rentalAdminClient.resolveDispute(eq(1L), any(ResolveDisputeRequest.class)))
                .thenReturn(ApiResponse.success());

        ResolveDisputeRequest req = new ResolveDisputeRequest();
        req.setAdminRemark("无 adminId");

        mvc.perform(put("/api/admin/disputes/1/resolve")
                        .header("X-User-Id", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        ArgumentCaptor<ResolveDisputeRequest> captor = ArgumentCaptor.forClass(ResolveDisputeRequest.class);
        verify(rentalAdminClient).resolveDispute(eq(1L), captor.capture());
        assertThat(captor.getValue().getAdminId()).isEqualTo(999L);
    }
}
