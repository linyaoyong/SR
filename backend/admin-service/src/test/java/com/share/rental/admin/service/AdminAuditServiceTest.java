package com.share.rental.admin.service;

import com.share.rental.admin.client.AuthAdminClient;
import com.share.rental.admin.client.ItemAdminClient;
import com.share.rental.admin.client.MessageAdminClient;
import com.share.rental.admin.dto.AdminAuditRequest;
import com.share.rental.admin.dto.AdminBanRequest;
import com.share.rental.admin.dto.AdminDashboardResponse;
import com.share.rental.admin.dto.AdminLogResponse;
import com.share.rental.admin.entity.AdminOperationLog;
import com.share.rental.admin.entity.AuditRecord;
import com.share.rental.admin.mapper.AdminOperationLogMapper;
import com.share.rental.admin.mapper.AuditRecordMapper;
import com.share.rental.auth.dto.BanUserRequest;
import com.share.rental.auth.dto.UserAdminStatsResponse;
import com.share.rental.auth.dto.UserAuditItemResponse;
import com.share.rental.auth.dto.UserAuditRequest;
import com.share.rental.common.response.ApiResponse;
import com.share.rental.item.dto.ItemAdminStatsResponse;
import com.share.rental.item.dto.ItemAuditActionResponse;
import com.share.rental.item.dto.ItemAuditResponse;
import com.share.rental.message.dto.SystemNotificationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditServiceTest {

    @Mock
    private AuthAdminClient authAdminClient;
    @Mock
    private ItemAdminClient itemAdminClient;
    @Mock
    private MessageAdminClient messageAdminClient;
    @Mock
    private AuditRecordMapper auditRecordMapper;
    @Mock
    private AdminOperationLogMapper operationLogMapper;

    @InjectMocks
    private AdminAuditService adminAuditService;

    @Test
    void listUserAudits_callsAuthClient() {
        when(authAdminClient.listAudits(eq(2)))
                .thenReturn(ApiResponse.success(List.of(
                        new UserAuditItemResponse(10L, "alice", "username", 2, "不合规",
                                null, null, 100, 0, 0, 2, 1, 1,
                                List.of("username"), List.of(2)))));

        var result = adminAuditService.listUserAudits(2);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(10L);
    }

    @Test
    void auditUser_callsAuthAndWritesRecordAndLog() {
        AdminAuditRequest request = new AdminAuditRequest(1, null, "username");
        when(authAdminClient.auditUser(eq(10L), eq(1L), any(UserAuditRequest.class)))
                .thenReturn(ApiResponse.success());

        adminAuditService.auditUser(1L, 10L, request, "127.0.0.1");

        verify(authAdminClient).auditUser(eq(10L), eq(1L), argThat(r ->
                r.getFieldName().equals("username") && r.getAuditStatus().equals(1)));
        verify(auditRecordMapper).insert(argThat((AuditRecord r) ->
                r.getAdminId().equals(1L)
                        && r.getTargetType().equals("USER")
                        && r.getTargetId().equals(10L)
                        && r.getNewStatus().equals(1)));
        verify(operationLogMapper).insert(argThat((AdminOperationLog l) ->
                l.getOperationType().equals("AUDIT_USER")
                        && l.getTargetType().equals("USER")
                        && l.getTargetId().equals(10L)
                        && l.getAdminId().equals(1L)
                        && l.getIp().equals("127.0.0.1")));
    }

    @Test
    void banUser_callsAuthAndWritesLog() {
        when(authAdminClient.banUser(eq(10L), eq(1L), any(BanUserRequest.class)))
                .thenReturn(ApiResponse.success());

        adminAuditService.banUser(1L, 10L, new AdminBanRequest("违规"), "127.0.0.1");

        verify(authAdminClient).banUser(eq(10L), eq(1L), argThat(r -> r.getReason().equals("违规")));
        verify(operationLogMapper).insert(argThat((AdminOperationLog l) ->
                l.getOperationType().equals("BAN_USER")
                        && l.getTargetType().equals("USER")
                        && l.getTargetId().equals(10L)));
    }

    @Test
    void unbanUser_callsAuthAndWritesLog() {
        when(authAdminClient.unbanUser(eq(10L), eq(1L)))
                .thenReturn(ApiResponse.success());

        adminAuditService.unbanUser(1L, 10L, "127.0.0.1");

        verify(authAdminClient).unbanUser(eq(10L), eq(1L));
        verify(operationLogMapper).insert(argThat((AdminOperationLog l) ->
                l.getOperationType().equals("UNBAN_USER")
                        && l.getTargetType().equals("USER")
                        && l.getTargetId().equals(10L)));
    }

    @Test
    void listItemAudits_callsItemClient() {
        when(itemAdminClient.listAudits(eq(2)))
                .thenReturn(ApiResponse.success(List.of(
                        new ItemAuditResponse(99L, 7L, "drill", 1, 2, "图片不清晰", null, 1L,
                                "描述", 3L, "工具", "电钻", 1,
                                new java.math.BigDecimal("50.00"), new java.math.BigDecimal("100.00"),
                                2, java.util.List.of("https://example.com/a.jpg")))));

        var result = adminAuditService.listItemAudits(2);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(99L);
    }

    @Test
    void auditItem_rectify_writesAuditRecordLogAndNotification() {
        AdminAuditRequest request = new AdminAuditRequest(2, "图片不清晰", null);
        when(itemAdminClient.auditItem(eq(99L), eq(1L), any()))
                .thenReturn(ApiResponse.success(new ItemAuditActionResponse(
                        99L, 7L, 2, "图片不清晰", LocalDateTime.now(), 1L)));

        adminAuditService.auditItem(1L, 99L, request, "127.0.0.1");

        verify(itemAdminClient).auditItem(eq(99L), eq(1L), any());
        verify(auditRecordMapper).insert(argThat((AuditRecord r) ->
                r.getAdminId().equals(1L)
                        && r.getTargetType().equals("ITEM")
                        && r.getTargetId().equals(99L)
                        && r.getNewStatus().equals(2)
                        && r.getReason().equals("图片不清晰")));
        verify(operationLogMapper).insert(argThat((AdminOperationLog l) ->
                l.getOperationType().equals("AUDIT_ITEM")
                        && l.getTargetType().equals("ITEM")
                        && l.getTargetId().equals(99L)));
        verify(messageAdminClient).createSystemNotification(argThat((SystemNotificationRequest n) ->
                n.getReceiverId().equals(7L)
                        && n.getMessageType().equals(5)
                        && n.getContent().contains("图片不清晰")));
    }

    @Test
    void auditItem_approve_writesRecordAndLogButNoNotification() {
        AdminAuditRequest request = new AdminAuditRequest(1, null, null);
        when(itemAdminClient.auditItem(eq(99L), eq(1L), any()))
                .thenReturn(ApiResponse.success(new ItemAuditActionResponse(
                        99L, 7L, 1, null, LocalDateTime.now(), 1L)));

        adminAuditService.auditItem(1L, 99L, request, "127.0.0.1");

        verify(itemAdminClient).auditItem(eq(99L), eq(1L), any());
        verify(auditRecordMapper).insert(argThat((AuditRecord r) ->
                r.getNewStatus().equals(1)));
        verify(operationLogMapper).insert(argThat((AdminOperationLog l) ->
                l.getOperationType().equals("AUDIT_ITEM")));
        verify(messageAdminClient, org.mockito.Mockito.never())
                .createSystemNotification(any());
    }

    @Test
    void forceOffShelf_callsItemClientAndWritesLog() {
        when(itemAdminClient.forceOffShelf(eq(99L)))
                .thenReturn(ApiResponse.success());

        adminAuditService.forceOffShelf(1L, 99L, "127.0.0.1");

        verify(itemAdminClient).forceOffShelf(eq(99L));
        verify(operationLogMapper).insert(argThat((AdminOperationLog l) ->
                l.getOperationType().equals("FORCE_OFF_SHELF")
                        && l.getTargetType().equals("ITEM")
                        && l.getTargetId().equals(99L)
                        && l.getAdminId().equals(1L)));
    }

    @Test
    void getDashboard_returnsCounts() {
        when(authAdminClient.listAudits(any()))
                .thenReturn(ApiResponse.success(List.of(
                        new UserAuditItemResponse(1L, "u", "username", 0, null,
                                null, null, 100, 0, 0, 0, 1, 1,
                                List.of("username"), List.of(0)),
                        new UserAuditItemResponse(2L, "v", "username", 0, null,
                                null, null, 100, 0, 0, 0, 1, 1,
                                List.of("username"), List.of(0)))));
        when(itemAdminClient.listAudits(any()))
                .thenReturn(ApiResponse.success(List.of(
                        new ItemAuditResponse(1L, 1L, "i", 1, 0, null, null, 1L,
                                null, null, null, null, 0, null, null, 1, null))));
        when(authAdminClient.getStats()).thenReturn(ApiResponse.success(
                new UserAdminStatsResponse(50L, 3L)));
        when(itemAdminClient.getStats()).thenReturn(ApiResponse.success(
                new ItemAdminStatsResponse(20L)));

        AdminDashboardResponse dashboard = adminAuditService.getDashboard();

        assertThat(dashboard.getUserAuditCount()).isEqualTo(2L);
        assertThat(dashboard.getItemAuditCount()).isEqualTo(1L);
        assertThat(dashboard.getTotalUsers()).isEqualTo(50L);
        assertThat(dashboard.getTotalItems()).isEqualTo(20L);
        assertThat(dashboard.getBannedUsers()).isEqualTo(3L);
    }

    @Test
    void getDashboard_fallsBackToZeroWhenStatsFail() {
        when(authAdminClient.listAudits(any())).thenReturn(ApiResponse.success(List.of()));
        when(itemAdminClient.listAudits(any())).thenReturn(ApiResponse.success(List.of()));
        when(authAdminClient.getStats()).thenThrow(new RuntimeException("auth down"));
        when(itemAdminClient.getStats()).thenThrow(new RuntimeException("item down"));

        AdminDashboardResponse dashboard = adminAuditService.getDashboard();

        assertThat(dashboard.getTotalUsers()).isEqualTo(0L);
        assertThat(dashboard.getTotalItems()).isEqualTo(0L);
        assertThat(dashboard.getBannedUsers()).isEqualTo(0L);
        assertThat(dashboard.getUserAuditCount()).isEqualTo(0L);
        assertThat(dashboard.getItemAuditCount()).isEqualTo(0L);
    }

    @Test
    void listLogs_returnsMappedLogs() {
        AdminOperationLog log = new AdminOperationLog();
        log.setId(1L);
        log.setAdminId(7L);
        log.setOperationType("AUDIT_ITEM");
        log.setTargetType("ITEM");
        log.setTargetId(99L);
        log.setRemark("ok");
        log.setIp("127.0.0.1");
        log.setCreateTime(LocalDateTime.of(2026, 6, 27, 12, 0));
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<AdminOperationLog> pageResult =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>();
        pageResult.setRecords(java.util.List.of(log));
        when(operationLogMapper.selectPage(any(), any())).thenReturn(pageResult);

        List<AdminLogResponse> result = adminAuditService.listLogs(1, 10);

        assertThat(result).hasSize(1);
        AdminLogResponse resp = result.get(0);
        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getAdminId()).isEqualTo(7L);
        assertThat(resp.getOperationType()).isEqualTo("AUDIT_ITEM");
        assertThat(resp.getTargetType()).isEqualTo("ITEM");
        assertThat(resp.getTargetId()).isEqualTo(99L);
        assertThat(resp.getIp()).isEqualTo("127.0.0.1");
    }
}
