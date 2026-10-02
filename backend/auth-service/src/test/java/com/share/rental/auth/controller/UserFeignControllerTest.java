package com.share.rental.auth.controller;

import com.share.rental.auth.service.UserService;
import com.share.rental.common.exception.GlobalExceptionHandler;
import com.share.rental.common.upload.UploadProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserFeignController.class)
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(UploadProperties.class)
class UserFeignControllerTest {

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

    @MockBean
    private UserService userService;

    @Test
    void auditUser_deserializesJsonBody() throws Exception {
        doNothing().when(userService).auditUser(eq(1L), eq(10L), argThat(request ->
                "username".equals(request.getFieldName())
                        && Integer.valueOf(2).equals(request.getAuditStatus())
                        && "昵称违规".equals(request.getReason())));

        mvc.perform(post("/internal/admin/users/10/audit")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fieldName\":\"username\",\"auditStatus\":2,\"reason\":\"昵称违规\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(userService).auditUser(eq(1L), eq(10L), argThat(request ->
                "username".equals(request.getFieldName())
                        && Integer.valueOf(2).equals(request.getAuditStatus())
                        && "昵称违规".equals(request.getReason())));
    }

    @Test
    void banUser_deserializesJsonBody() throws Exception {
        doNothing().when(userService).banUser(eq(1L), eq(10L), argThat(request ->
                "spam".equals(request.getReason())));

        mvc.perform(post("/internal/admin/users/10/ban")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"spam\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(userService).banUser(eq(1L), eq(10L), argThat(request ->
                "spam".equals(request.getReason())));
    }

    @Test
    void banUser_allowsEmptyBody() throws Exception {
        doNothing().when(userService).banUser(eq(1L), eq(10L), argThat(request ->
                request.getReason() == null));

        mvc.perform(post("/internal/admin/users/10/ban")
                        .header("X-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(userService).banUser(eq(1L), eq(10L), argThat(request ->
                request.getReason() == null));
    }

    @Test
    void adjustCreditScore_deserializesJsonBody() throws Exception {
        doNothing().when(userService).adjustCreditScore(eq(10L), argThat(request ->
                Long.valueOf(99L).equals(request.getOrderId())
                        && Integer.valueOf(1).equals(request.getChangeValue())
                        && "ORDER_COMPLETED".equals(request.getReasonType())
                        && "完成订单".equals(request.getReason())));

        mvc.perform(post("/internal/users/10/credit-score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":99,\"changeValue\":1,\"reasonType\":\"ORDER_COMPLETED\",\"reason\":\"完成订单\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(userService).adjustCreditScore(eq(10L), argThat(request ->
                Long.valueOf(99L).equals(request.getOrderId())
                        && Integer.valueOf(1).equals(request.getChangeValue())
                        && "ORDER_COMPLETED".equals(request.getReasonType())
                        && "完成订单".equals(request.getReason())));
    }
}
