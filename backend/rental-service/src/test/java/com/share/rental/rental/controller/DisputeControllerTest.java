package com.share.rental.rental.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.rental.common.exception.GlobalExceptionHandler;
import com.share.rental.common.upload.UploadProperties;
import com.share.rental.rental.dto.DisputeCreateRequest;
import com.share.rental.rental.dto.DisputeResponse;
import com.share.rental.rental.service.DisputeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DisputeController.class)
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(UploadProperties.class)
class DisputeControllerTest {

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
    private DisputeService disputeService;

    @Test
    void createDispute_returns200() throws Exception {
        when(disputeService.createDispute(eq(1L), eq(100L), any(DisputeCreateRequest.class)))
                .thenReturn(disputeResponse(1L));

        DisputeCreateRequest req = new DisputeCreateRequest();
        req.setReason("物品损坏");
        req.setDescription("屏幕有划痕");
        req.setExpectedDepositDeduction(new BigDecimal("50.00"));

        mvc.perform(post("/api/orders/1/disputes")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void createDispute_missingReason_returns400() throws Exception {
        DisputeCreateRequest req = new DisputeCreateRequest();

        mvc.perform(post("/api/orders/1/disputes")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40004));
    }

    @Test
    void listDisputes_returns200() throws Exception {
        when(disputeService.listDisputes(eq(1L), eq(100L)))
                .thenReturn(List.of(disputeResponse(1L)));

        mvc.perform(get("/api/orders/1/disputes")
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    private DisputeResponse disputeResponse(Long id) {
        DisputeResponse resp = new DisputeResponse();
        resp.setId(id);
        resp.setOrderId(1L);
        resp.setApplicantId(100L);
        resp.setReason("物品损坏");
        resp.setDescription("屏幕有划痕");
        resp.setExpectedDepositDeduction(new BigDecimal("50.00"));
        resp.setStatus(0);
        return resp;
    }
}
