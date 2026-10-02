package com.share.rental.item.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.rental.common.exception.GlobalExceptionHandler;
import com.share.rental.common.upload.UploadProperties;
import com.share.rental.item.dto.ItemAuditActionResponse;
import com.share.rental.item.dto.ItemAuditRequest;
import com.share.rental.item.dto.ItemAuditResponse;
import com.share.rental.item.service.ItemInternalService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证 ItemFeignController 内部接口契约：
 * - audit 端点从 X-User-Id 头读取 adminId（Task 4 遗留 I-1：曾用 X-Admin-Id，现统一为 X-User-Id）
 */
@WebMvcTest(ItemFeignController.class)
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(UploadProperties.class)
class ItemFeignControllerTest {

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
    private ItemInternalService itemInternalService;

    @Test
    void audit_readsAdminIdFromXUserHeader() throws Exception {
        ItemAuditRequest req = new ItemAuditRequest();
        req.setAuditStatus(2);
        req.setAuditReason("图片不清晰");
        when(itemInternalService.auditItem(eq(99L), eq(2), eq("图片不清晰"), eq(7L)))
                .thenReturn(new ItemAuditActionResponse(99L, 10L, 2, "图片不清晰",
                        LocalDateTime.of(2026, 6, 27, 12, 0), 7L));

        mvc.perform(post("/internal/admin/items/99/audit")
                        .header("X-User-Id", 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.itemId").value(99))
                .andExpect(jsonPath("$.data.ownerId").value(10))
                .andExpect(jsonPath("$.data.auditAdminId").value(7));

        verify(itemInternalService).auditItem(eq(99L), eq(2), eq("图片不清晰"), eq(7L));
    }

    @Test
    void audits_returnsList() throws Exception {
        when(itemInternalService.listAuditItems(eq(2)))
                .thenReturn(List.of(new ItemAuditResponse(
                        99L, 10L, "drill", 1, 2, "图片不清晰", null, 7L,
                        "电钻描述", 3L, "工具", "电钻,DIY", 1,
                        new java.math.BigDecimal("50.00"), new java.math.BigDecimal("100.00"),
                        2, List.of("https://example.com/drill.jpg"))));

        mvc.perform(get("/internal/admin/items/audits").param("auditStatus", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(99))
                .andExpect(jsonPath("$.data[0].description").value("电钻描述"))
                .andExpect(jsonPath("$.data[0].categoryName").value("工具"))
                .andExpect(jsonPath("$.data[0].imageUrls[0]").value("https://example.com/drill.jpg"));
    }

    @Test
    void forceOffShelf_callsService() throws Exception {
        org.mockito.Mockito.doNothing().when(itemInternalService).forceOffShelf(eq(99L));

        mvc.perform(post("/internal/admin/items/99/force-off-shelf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(itemInternalService).forceOffShelf(eq(99L));
    }
}
