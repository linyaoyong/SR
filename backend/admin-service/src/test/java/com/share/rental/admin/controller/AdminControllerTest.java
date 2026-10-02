package com.share.rental.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.rental.admin.dto.AdminAuditRequest;
import com.share.rental.admin.dto.AdminBanRequest;
import com.share.rental.admin.dto.AdminDashboardResponse;
import com.share.rental.admin.dto.AdminLogResponse;
import com.share.rental.admin.service.AdminAuditService;
import com.share.rental.auth.dto.UserAuditItemResponse;
import com.share.rental.common.exception.GlobalExceptionHandler;
import com.share.rental.common.upload.UploadProperties;
import com.share.rental.item.dto.ItemAuditActionResponse;
import com.share.rental.item.dto.ItemAuditResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class)
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(UploadProperties.class)
class AdminControllerTest {

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
    private AdminAuditService adminAuditService;

    @Test
    void dashboard_returnsSuccess() throws Exception {
        when(adminAuditService.getDashboard())
                .thenReturn(new AdminDashboardResponse(1L, 2L, 10L, 20L, 1L));

        mvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.itemAuditCount").value(1))
                .andExpect(jsonPath("$.data.userAuditCount").value(2));
    }

    @Test
    void listUserAudits_returnsSuccess() throws Exception {
        when(adminAuditService.listUserAudits(eq(2)))
                .thenReturn(List.of(new UserAuditItemResponse(10L, "alice", "username", 2, "不合规",
                        null, null, 100, 0, 0, 2, 1, 1,
                        List.of("username"), List.of(2))));

        mvc.perform(get("/api/admin/users/audits").param("auditStatus", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].userId").value(10));
    }

    @Test
    void auditUser_returnsSuccess() throws Exception {
        doNothing().when(adminAuditService).auditUser(eq(1L), eq(10L), any(AdminAuditRequest.class), any());

        AdminAuditRequest req = new AdminAuditRequest(1, null, "username");

        mvc.perform(post("/api/admin/users/10/audit")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(adminAuditService).auditUser(eq(1L), eq(10L), any(AdminAuditRequest.class), any());
    }

    @Test
    void auditUser_missingAdminIdHeader_returns400() throws Exception {
        AdminAuditRequest req = new AdminAuditRequest(1, null, "username");

        mvc.perform(post("/api/admin/users/10/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void banUser_returnsSuccess() throws Exception {
        doNothing().when(adminAuditService).banUser(eq(1L), eq(10L), any(AdminBanRequest.class), any());

        AdminBanRequest req = new AdminBanRequest("违规");

        mvc.perform(post("/api/admin/users/10/ban")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void unbanUser_returnsSuccess() throws Exception {
        doNothing().when(adminAuditService).unbanUser(eq(1L), eq(10L), any());

        mvc.perform(post("/api/admin/users/10/unban")
                        .header("X-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void listItemAudits_returnsSuccess() throws Exception {
        when(adminAuditService.listItemAudits(eq(2)))
                .thenReturn(List.of(new ItemAuditResponse(99L, 7L, "drill", 1, 2, "图片不清晰", null, 1L,
                        "描述", 3L, "工具", "电钻", 1,
                        new java.math.BigDecimal("50.00"), new java.math.BigDecimal("100.00"),
                        2, java.util.List.of("https://example.com/a.jpg"))));

        mvc.perform(get("/api/admin/items/audits").param("auditStatus", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(99));
    }

    @Test
    void auditItem_returnsSuccess() throws Exception {
        when(adminAuditService.auditItem(eq(1L), eq(99L), any(AdminAuditRequest.class), any()))
                .thenReturn(new ItemAuditActionResponse(99L, 7L, 2, "图片不清晰", LocalDateTime.now(), 1L));

        AdminAuditRequest req = new AdminAuditRequest(2, "图片不清晰", null);

        mvc.perform(post("/api/admin/items/99/audit")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.itemId").value(99))
                .andExpect(jsonPath("$.data.ownerId").value(7));
    }

    @Test
    void forceOffShelf_returnsSuccess() throws Exception {
        doNothing().when(adminAuditService).forceOffShelf(eq(1L), eq(99L), any());

        mvc.perform(post("/api/admin/items/99/force-off-shelf")
                        .header("X-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void listLogs_returnsSuccess() throws Exception {
        when(adminAuditService.listLogs(anyInt(), anyInt()))
                .thenReturn(List.of(new AdminLogResponse(
                        1L, 7L, "AUDIT_ITEM", "ITEM", 99L, "ok", "127.0.0.1",
                        LocalDateTime.of(2026, 6, 27, 12, 0))));

        mvc.perform(get("/api/admin/logs").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].operationType").value("AUDIT_ITEM"));
    }
}
