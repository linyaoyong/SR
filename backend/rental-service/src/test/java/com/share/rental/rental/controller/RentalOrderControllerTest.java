package com.share.rental.rental.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.exception.GlobalExceptionHandler;
import com.share.rental.common.upload.UploadProperties;
import com.share.rental.rental.dto.CancelOrderRequest;
import com.share.rental.rental.dto.FreezeDepositOrderRequest;
import com.share.rental.rental.dto.PayOrderRequest;
import com.share.rental.rental.dto.RentalOrderResponse;
import com.share.rental.rental.dto.ReturnOrderRequest;
import com.share.rental.rental.dto.ShipOrderRequest;
import com.share.rental.rental.service.RentalOrderService;
import org.junit.jupiter.api.Test;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RentalOrderController.class)
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(UploadProperties.class)
class RentalOrderControllerTest {

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
    private RentalOrderService rentalOrderService;

    @Test
    void list_returnsPageOfOrders() throws Exception {
        when(rentalOrderService.listOrders(eq(10L)))
                .thenReturn(List.of(orderResponse(1L)));

        mvc.perform(get("/api/orders")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void detail_returnsOrderDetail() throws Exception {
        when(rentalOrderService.getOrder(eq(1L), eq(10L)))
                .thenReturn(orderResponse(1L));

        mvc.perform(get("/api/orders/1")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void detail_notFound_returnsOrderNotFoundCode() throws Exception {
        doThrow(new BusinessException(ErrorCode.RENTAL_ORDER_NOT_FOUND))
                .when(rentalOrderService).getOrder(eq(1L), any());

        mvc.perform(get("/api/orders/1")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40306));
    }

    @Test
    void pay_returnsUpdatedOrder() throws Exception {
        when(rentalOrderService.payOrder(eq(1L), eq(10L), any(BigDecimal.class)))
                .thenReturn(orderResponse(1L));

        PayOrderRequest req = new PayOrderRequest();
        req.setAmount(new BigDecimal("60.00"));

        mvc.perform(post("/api/orders/1/pay")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void pay_missingAmount_returns400() throws Exception {
        PayOrderRequest req = new PayOrderRequest();

        mvc.perform(post("/api/orders/1/pay")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40004));
    }

    @Test
    void freezeDeposit_returnsUpdatedOrder() throws Exception {
        when(rentalOrderService.freezeDeposit(eq(1L), eq(10L), any(BigDecimal.class)))
                .thenReturn(orderResponse(1L));

        FreezeDepositOrderRequest req = new FreezeDepositOrderRequest();
        req.setAmount(new BigDecimal("100.00"));

        mvc.perform(post("/api/orders/1/freeze-deposit")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void freezeDeposit_missingAmount_returns400() throws Exception {
        FreezeDepositOrderRequest req = new FreezeDepositOrderRequest();

        mvc.perform(post("/api/orders/1/freeze-deposit")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40004));
    }

    @Test
    void cancel_validBody_returnsSuccess() throws Exception {
        doNothing().when(rentalOrderService).cancel(eq(1L), eq(10L), any(CancelOrderRequest.class));

        mvc.perform(post("/api/orders/1/cancel")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelOrderRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void cancel_statusInvalid_returnsStatusInvalidCode() throws Exception {
        doThrow(new BusinessException(ErrorCode.RENTAL_ORDER_STATUS_INVALID))
                .when(rentalOrderService).cancel(eq(1L), eq(10L), any(CancelOrderRequest.class));

        mvc.perform(post("/api/orders/1/cancel")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelOrderRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40307));
    }

    @Test
    void ship_validBody_returnsSuccess() throws Exception {
        doNothing().when(rentalOrderService).ship(eq(1L), eq(10L), any(ShipOrderRequest.class));

        ShipOrderRequest req = new ShipOrderRequest();
        req.setShipCompany("顺丰");
        req.setShipTrackingNo("SF1234567890");

        mvc.perform(post("/api/orders/1/ship")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void ship_missingCompany_returnsValidationError() throws Exception {
        ShipOrderRequest req = new ShipOrderRequest();

        mvc.perform(post("/api/orders/1/ship")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40004));
    }

    @Test
    void receive_returnsSuccess() throws Exception {
        doNothing().when(rentalOrderService).receive(eq(1L), eq(10L));

        mvc.perform(post("/api/orders/1/receive")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void returnOrder_returnsSuccess() throws Exception {
        doNothing().when(rentalOrderService).returnOrder(eq(1L), eq(10L), any(ReturnOrderRequest.class));

        ReturnOrderRequest req = new ReturnOrderRequest();
        req.setReturnCompany("顺丰");
        req.setReturnTrackingNo("SF9876543210");

        mvc.perform(post("/api/orders/1/return")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void returnOrder_emptyBody_returnsSuccessForMeetupOrders() throws Exception {
        doNothing().when(rentalOrderService).returnOrder(eq(1L), eq(10L), any());

        mvc.perform(post("/api/orders/1/return")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void complete_returnsSuccess() throws Exception {
        doNothing().when(rentalOrderService).complete(eq(1L), eq(10L));

        mvc.perform(post("/api/orders/1/complete")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private RentalOrderResponse orderResponse(Long id) {
        RentalOrderResponse resp = new RentalOrderResponse();
        resp.setId(id);
        resp.setOrderNo("SR20260627120000" + id);
        resp.setApplicationId(100L);
        resp.setProposalId(200L);
        resp.setItemId(50L);
        resp.setItemSnapshotId(500L);
        resp.setOwnerId(20L);
        resp.setRenterId(10L);
        resp.setQuantity(1);
        resp.setDeliveryType(1);
        resp.setRentStartTime(LocalDateTime.of(2026, 7, 1, 10, 0));
        resp.setRentEndTime(LocalDateTime.of(2026, 7, 3, 10, 0));
        resp.setDailyPrice(new BigDecimal("20.00"));
        resp.setRentAmount(new BigDecimal("60.00"));
        resp.setDepositAmount(new BigDecimal("100.00"));
        resp.setPaidRentAmount(BigDecimal.ZERO);
        resp.setFrozenDepositAmount(BigDecimal.ZERO);
        resp.setStatus(0);
        resp.setCreateTime(LocalDateTime.now());
        return resp;
    }
}
