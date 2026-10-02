package com.share.rental.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.rental.common.exception.GlobalExceptionHandler;
import com.share.rental.common.upload.UploadProperties;
import com.share.rental.wallet.dto.RechargeRequest;
import com.share.rental.wallet.dto.WalletMeResponse;
import com.share.rental.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(UploadProperties.class)
class WalletControllerTest {

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
    private WalletService walletService;

    @Test
    void me_returnsWallet() throws Exception {
        when(walletService.getMyWallet(10L)).thenReturn(
                new WalletMeResponse(10L, new BigDecimal("100.00"), BigDecimal.ZERO, 0));

        mvc.perform(get("/api/wallet/me")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.balance").value(100.00));
    }

    @Test
    void recharge_validAmount_returnsSuccess() throws Exception {
        RechargeRequest req = new RechargeRequest();
        req.setAmount(new BigDecimal("50.00"));
        req.setRemark("test");
        doNothing().when(walletService).recharge(eq(10L), any(BigDecimal.class), any());

        mvc.perform(post("/api/wallet/recharge")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void recharge_zeroAmount_returnsValidationError() throws Exception {
        RechargeRequest req = new RechargeRequest();
        req.setAmount(new BigDecimal("0.00"));

        mvc.perform(post("/api/wallet/recharge")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40004));
    }
}
