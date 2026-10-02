package com.share.rental.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.rental.common.exception.GlobalExceptionHandler;
import com.share.rental.common.upload.UploadProperties;
import com.share.rental.wallet.dto.CancelOrderPaymentRequest;
import com.share.rental.wallet.dto.CompleteSettlementRequest;
import com.share.rental.wallet.dto.FreezeDepositRequest;
import com.share.rental.wallet.dto.PrepayRequest;
import com.share.rental.wallet.dto.SettlementResponse;
import com.share.rental.wallet.dto.WalletOperationResponse;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletFeignController.class)
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(UploadProperties.class)
class WalletFeignControllerTest {

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
    void prepayRent_callsServiceAndReturnsResponse() throws Exception {
        when(walletService.prepayRent(any(PrepayRequest.class)))
                .thenReturn(new WalletOperationResponse(1L, new BigDecimal("100.00"), BigDecimal.ZERO, false));

        PrepayRequest req = new PrepayRequest();
        req.setRenterId(10L);
        req.setAmount(new BigDecimal("100.00"));

        mvc.perform(post("/internal/wallet/orders/1/prepay-rent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderId").value(1))
                .andExpect(jsonPath("$.data.paidRentAmount").value(100.00));

        org.mockito.ArgumentCaptor<PrepayRequest> captor =
                org.mockito.ArgumentCaptor.forClass(PrepayRequest.class);
        verify(walletService).prepayRent(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(1L, captor.getValue().getOrderId());
    }

    @Test
    void freezeDeposit_callsServiceAndReturnsResponse() throws Exception {
        when(walletService.freezeDeposit(any(FreezeDepositRequest.class)))
                .thenReturn(new WalletOperationResponse(1L, BigDecimal.ZERO, new BigDecimal("50.00"), false));

        FreezeDepositRequest req = new FreezeDepositRequest();
        req.setRenterId(10L);
        req.setAmount(new BigDecimal("50.00"));

        mvc.perform(post("/internal/wallet/orders/1/freeze-deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderId").value(1))
                .andExpect(jsonPath("$.data.frozenDepositAmount").value(50.00));

        org.mockito.ArgumentCaptor<FreezeDepositRequest> captor =
                org.mockito.ArgumentCaptor.forClass(FreezeDepositRequest.class);
        verify(walletService).freezeDeposit(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(1L, captor.getValue().getOrderId());
    }

    @Test
    void cancel_callsServiceAndReturnsResponse() throws Exception {
        when(walletService.cancelOrderPayment(any(CancelOrderPaymentRequest.class)))
                .thenReturn(new WalletOperationResponse(1L, BigDecimal.ZERO, BigDecimal.ZERO, false));

        CancelOrderPaymentRequest req = new CancelOrderPaymentRequest();
        req.setRenterId(10L);
        req.setOwnerId(20L);
        req.setPaidRentAmount(new BigDecimal("100.00"));
        req.setFrozenDepositAmount(new BigDecimal("50.00"));

        mvc.perform(post("/internal/wallet/orders/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderId").value(1));

        org.mockito.ArgumentCaptor<CancelOrderPaymentRequest> captor =
                org.mockito.ArgumentCaptor.forClass(CancelOrderPaymentRequest.class);
        verify(walletService).cancelOrderPayment(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(1L, captor.getValue().getOrderId());
    }

    @Test
    void settle_callsServiceAndReturnsResponse() throws Exception {
        when(walletService.completeSettlement(any(CompleteSettlementRequest.class)))
                .thenReturn(new SettlementResponse(1L, true, true, BigDecimal.ZERO, BigDecimal.ZERO));

        CompleteSettlementRequest req = new CompleteSettlementRequest();
        req.setRenterId(10L);
        req.setOwnerId(20L);
        req.setRentAmount(new BigDecimal("200.00"));
        req.setDepositAmount(new BigDecimal("100.00"));

        mvc.perform(post("/internal/wallet/orders/1/settle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderId").value(1))
                .andExpect(jsonPath("$.data.rentSettled").value(true))
                .andExpect(jsonPath("$.data.depositReleased").value(true));

        org.mockito.ArgumentCaptor<CompleteSettlementRequest> captor =
                org.mockito.ArgumentCaptor.forClass(CompleteSettlementRequest.class);
        verify(walletService).completeSettlement(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(1L, captor.getValue().getOrderId());
    }

    @Test
    void getSettlement_returnsExistingRecord() throws Exception {
        when(walletService.getSettlement(1L))
                .thenReturn(new SettlementResponse(1L, true, true,
                        new BigDecimal("7.50"), new BigDecimal("7.50")));

        mvc.perform(get("/internal/wallet/orders/1/settlement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderId").value(1))
                .andExpect(jsonPath("$.data.rentSettled").value(true))
                .andExpect(jsonPath("$.data.overdueFeeAmount").value(7.50))
                .andExpect(jsonPath("$.data.depositDeductedAmount").value(7.50));

        verify(walletService).getSettlement(1L);
    }
}
