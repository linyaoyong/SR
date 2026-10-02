package com.share.rental.rental.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.exception.GlobalExceptionHandler;
import com.share.rental.common.upload.UploadProperties;
import com.share.rental.rental.dto.RentalApplicationCreateRequest;
import com.share.rental.rental.dto.RentalApplicationDetailResponse;
import com.share.rental.rental.dto.RentalApplicationResponse;
import com.share.rental.rental.dto.RentalProposalResponse;
import com.share.rental.rental.dto.RentalProposalUpdateRequest;
import com.share.rental.rental.service.RentalApplicationService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RentalApplicationController.class)
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(UploadProperties.class)
class RentalApplicationControllerTest {

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
    private RentalApplicationService rentalApplicationService;

    @Test
    void create_validBody_returnsSuccess() throws Exception {
        when(rentalApplicationService.createApplication(eq(10L), any(RentalApplicationCreateRequest.class)))
                .thenReturn(detailResponse(100L));

        mvc.perform(post("/api/rentals/applications")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(100));
    }

    @Test
    void create_missingQuantity_returnsValidationError() throws Exception {
        RentalApplicationCreateRequest req = new RentalApplicationCreateRequest();

        mvc.perform(post("/api/rentals/applications")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40004));
    }

    @Test
    void list_returnsPageOfApplications() throws Exception {
        when(rentalApplicationService.listApplications(eq(10L)))
                .thenReturn(List.of(applicationResponse(1L)));

        mvc.perform(get("/api/rentals/applications")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void detail_returnsApplicationDetail() throws Exception {
        when(rentalApplicationService.getApplication(eq(1L), eq(10L)))
                .thenReturn(detailResponse(1L));

        mvc.perform(get("/api/rentals/applications/1")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void detail_notFound_returnsRentalApplicationNotFoundCode() throws Exception {
        doThrow(new BusinessException(ErrorCode.RENTAL_APPLICATION_NOT_FOUND))
                .when(rentalApplicationService).getApplication(eq(1L), any());

        mvc.perform(get("/api/rentals/applications/1")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void updateProposal_validBody_returnsSuccess() throws Exception {
        when(rentalApplicationService.updateProposal(eq(1L), eq(10L), any(RentalProposalUpdateRequest.class)))
                .thenReturn(detailResponse(1L));

        mvc.perform(put("/api/rentals/applications/1/proposal")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RentalProposalUpdateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void confirm_returnsSuccess() throws Exception {
        when(rentalApplicationService.confirm(eq(1L), eq(10L)))
                .thenReturn(detailResponse(1L));

        mvc.perform(put("/api/rentals/applications/1/confirm")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void cancel_returnsSuccess() throws Exception {
        doNothing().when(rentalApplicationService).cancel(eq(1L), eq(10L));

        mvc.perform(post("/api/rentals/applications/1/cancel")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void cancel_permissionDenied_returnsPermissionDeniedCode() throws Exception {
        doThrow(new BusinessException(ErrorCode.RENTAL_APPLICATION_PERMISSION_DENIED))
                .when(rentalApplicationService).cancel(eq(1L), eq(99L));

        mvc.perform(post("/api/rentals/applications/1/cancel")
                        .header("X-User-Id", 99L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40301));
    }

    private RentalApplicationResponse applicationResponse(Long id) {
        RentalApplicationResponse resp = new RentalApplicationResponse();
        resp.setId(id);
        resp.setItemId(50L);
        resp.setRenterId(10L);
        resp.setOwnerId(20L);
        resp.setStatus(0);
        resp.setOwnerConfirmed(0);
        resp.setRenterConfirmed(1);
        resp.setCreateTime(LocalDateTime.now());
        return resp;
    }

    private RentalApplicationDetailResponse detailResponse(Long id) {
        RentalApplicationDetailResponse resp = new RentalApplicationDetailResponse();
        resp.setId(id);
        resp.setItemId(50L);
        resp.setRenterId(10L);
        resp.setOwnerId(20L);
        resp.setStatus(0);
        resp.setOwnerConfirmed(0);
        resp.setRenterConfirmed(1);
        resp.setCreateTime(LocalDateTime.now());
        resp.setItemTitle("电钻");
        resp.setCurrentProposal(proposalResponse(id));
        return resp;
    }

    private RentalProposalResponse proposalResponse(Long applicationId) {
        RentalProposalResponse p = new RentalProposalResponse();
        p.setId(1L);
        p.setApplicationId(applicationId);
        p.setVersionNo(1);
        p.setOperatorId(10L);
        p.setQuantity(1);
        p.setDeliveryType(1);
        p.setRentStartTime(LocalDateTime.of(2026, 7, 1, 10, 0));
        p.setRentEndTime(LocalDateTime.of(2026, 7, 3, 10, 0));
        p.setRentAmount(new BigDecimal("60.00"));
        p.setDepositAmount(new BigDecimal("100.00"));
        p.setCreateTime(LocalDateTime.now());
        return p;
    }

    private RentalApplicationCreateRequest validCreateRequest() {
        RentalApplicationCreateRequest req = new RentalApplicationCreateRequest();
        req.setItemId(50L);
        req.setQuantity(1);
        req.setRentStartTime(LocalDateTime.of(2026, 7, 1, 10, 0));
        req.setRentEndTime(LocalDateTime.of(2026, 7, 3, 10, 0));
        req.setDeliveryType(1);
        req.setReceiverName("张三");
        req.setReceiverPhone("13800000000");
        req.setReceiverAddress("北京市朝阳区");
        req.setRentAmount(new BigDecimal("60.00"));
        req.setDepositAmount(new BigDecimal("100.00"));
        return req;
    }
}
