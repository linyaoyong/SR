package com.share.rental.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.response.ApiResponse;
import com.share.rental.item.dto.ItemAdminStatsResponse;
import com.share.rental.item.dto.ItemAuditActionResponse;
import com.share.rental.item.dto.ItemAuditRequest;
import com.share.rental.item.dto.ItemAuditResponse;
import com.share.rental.message.dto.SystemNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理端审核/封禁/强制下架/日志业务。
 * 通过 Feign 调用 auth-service、item-service、message-service，
 * 自身只访问 sr_admin 库（写 audit_records、admin_operation_logs）。
 */
@Service
public class AdminAuditService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuditService.class);

    /** 物品审核-要求整改 */
    private static final int AUDIT_STATUS_RECTIFY = 2;
    /** 系统通知 messageType：物品审核整改通知 */
    private static final int MESSAGE_TYPE_ITEM_RECTIFY = 5;

    private final AuthAdminClient authAdminClient;
    private final ItemAdminClient itemAdminClient;
    private final MessageAdminClient messageAdminClient;
    private final AuditRecordMapper auditRecordMapper;
    private final AdminOperationLogMapper operationLogMapper;

    @Autowired
    public AdminAuditService(AuthAdminClient authAdminClient,
                              ItemAdminClient itemAdminClient,
                              MessageAdminClient messageAdminClient,
                              AuditRecordMapper auditRecordMapper,
                              AdminOperationLogMapper operationLogMapper) {
        this.authAdminClient = authAdminClient;
        this.itemAdminClient = itemAdminClient;
        this.messageAdminClient = messageAdminClient;
        this.auditRecordMapper = auditRecordMapper;
        this.operationLogMapper = operationLogMapper;
    }

    public List<UserAuditItemResponse> listUserAudits(Integer auditStatus) {
        ApiResponse<List<UserAuditItemResponse>> resp = authAdminClient.listAudits(auditStatus);
        ensureOk(resp);
        return resp.data();
    }

    public void auditUser(Long adminId, Long userId, AdminAuditRequest request, String ip) {
        String fieldName = request.getFieldName() != null ? request.getFieldName() : "username";
        UserAuditRequest userAuditRequest = new UserAuditRequest(
                fieldName, request.getAuditStatus(), request.getAuditReason());
        ensureOk(authAdminClient.auditUser(userId, adminId, userAuditRequest));

        AuditRecord record = new AuditRecord();
        record.setAdminId(adminId);
        record.setTargetType("USER");
        record.setTargetId(userId);
        record.setFieldName(fieldName);
        record.setNewStatus(request.getAuditStatus());
        record.setReason(request.getAuditReason());
        auditRecordMapper.insert(record);

        operationLogMapper.insert(buildLog(adminId, "AUDIT_USER", "USER", userId,
                request.getAuditReason(), ip));
    }

    public void banUser(Long adminId, Long userId, AdminBanRequest request, String ip) {
        ensureOk(authAdminClient.banUser(userId, adminId, new BanUserRequest(request.getReason())));
        operationLogMapper.insert(buildLog(adminId, "BAN_USER", "USER", userId,
                request.getReason(), ip));
    }

    public void unbanUser(Long adminId, Long userId, String ip) {
        ensureOk(authAdminClient.unbanUser(userId, adminId));
        operationLogMapper.insert(buildLog(adminId, "UNBAN_USER", "USER", userId, null, ip));
    }

    public List<ItemAuditResponse> listItemAudits(Integer auditStatus) {
        ApiResponse<List<ItemAuditResponse>> resp = itemAdminClient.listAudits(auditStatus);
        ensureOk(resp);
        return resp.data();
    }

    public ItemAuditActionResponse auditItem(Long adminId, Long itemId,
                                              AdminAuditRequest request, String ip) {
        ItemAuditRequest itemAuditRequest = new ItemAuditRequest();
        itemAuditRequest.setAuditStatus(request.getAuditStatus());
        itemAuditRequest.setAuditReason(request.getAuditReason());

        ApiResponse<ItemAuditActionResponse> resp =
                itemAdminClient.auditItem(itemId, adminId, itemAuditRequest);
        ensureOk(resp);
        ItemAuditActionResponse action = resp.data();

        AuditRecord record = new AuditRecord();
        record.setAdminId(adminId);
        record.setTargetType("ITEM");
        record.setTargetId(itemId);
        record.setNewStatus(request.getAuditStatus());
        record.setReason(request.getAuditReason());
        auditRecordMapper.insert(record);

        operationLogMapper.insert(buildLog(adminId, "AUDIT_ITEM", "ITEM", itemId,
                request.getAuditReason(), ip));

        if (request.getAuditStatus() != null && request.getAuditStatus() == AUDIT_STATUS_RECTIFY
                && action != null && action.getOwnerId() != null) {
            // 通知文案判空回退：reason 为空时给用户兜底提示，避免通知变成「您的物品被要求整改：」
            String content = "您的物品被要求整改：" + (request.getAuditReason() == null
                    || request.getAuditReason().isBlank()
                    ? "请联系管理员了解详情"
                    : request.getAuditReason());
            // fire-and-forget：通知失败不影响审核结果与本地审计记录，避免重试导致重复写入
            try {
                messageAdminClient.createSystemNotification(new SystemNotificationRequest(
                        action.getOwnerId(), content, MESSAGE_TYPE_ITEM_RECTIFY));
            } catch (Exception e) {
                log.warn("发送物品整改通知失败 ownerId={}, itemId={}, status={}",
                        action.getOwnerId(), itemId, request.getAuditStatus(), e);
            }
        }
        return action;
    }

    public void forceOffShelf(Long adminId, Long itemId, String ip) {
        ensureOk(itemAdminClient.forceOffShelf(itemId));
        operationLogMapper.insert(buildLog(adminId, "FORCE_OFF_SHELF", "ITEM", itemId, null, ip));
    }

    public AdminDashboardResponse getDashboard() {
        // 缓存 Feign 结果，避免重复远程调用；字段语义为「审核记录计数」（P0 近似值，未按状态筛选）
        List<UserAuditItemResponse> userAudits = listUserAudits(null);
        long userAuditCount = userAudits == null ? 0 : userAudits.size();
        List<ItemAuditResponse> itemAudits = listItemAudits(null);
        long itemAuditCount = itemAudits == null ? 0 : itemAudits.size();
        UserAdminStatsResponse userStats = fetchUserStatsSafely();
        long totalUsers = valueOrZero(userStats == null ? null : userStats.getTotalUsers());
        long totalItems = fetchTotalItemsSafely();
        long bannedUsers = valueOrZero(userStats == null ? null : userStats.getBannedUsers());
        return new AdminDashboardResponse(
                itemAuditCount, userAuditCount, totalUsers, totalItems, bannedUsers);
    }

    /** fire-and-forget：用户统计失败时回退到 0，避免拖垮整个 dashboard。 */
    private UserAdminStatsResponse fetchUserStatsSafely() {
        try {
            ApiResponse<UserAdminStatsResponse> resp = authAdminClient.getStats();
            if (resp == null || resp.data() == null) {
                return null;
            }
            return resp.data();
        } catch (Exception e) {
            log.warn("获取用户统计失败，回退到 0: {}", e.getMessage());
            return null;
        }
    }

    private long fetchTotalItemsSafely() {
        try {
            ApiResponse<ItemAdminStatsResponse> resp = itemAdminClient.getStats();
            if (resp == null || resp.data() == null) {
                return 0L;
            }
            return resp.data().getTotalItems() == null ? 0L : resp.data().getTotalItems();
        } catch (Exception e) {
            log.warn("获取物品统计失败，回退到 0: {}", e.getMessage());
            return 0L;
        }
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    public List<AdminLogResponse> listLogs(int page, int size) {
        Page<AdminOperationLog> pageReq = new Page<>(page, size);
        LambdaQueryWrapper<AdminOperationLog> wrapper = new LambdaQueryWrapper<AdminOperationLog>()
                .orderByDesc(AdminOperationLog::getCreateTime);
        Page<AdminOperationLog> result = operationLogMapper.selectPage(pageReq, wrapper);
        return result.getRecords().stream().map(this::toLogResponse).toList();
    }

    private AdminOperationLog buildLog(Long adminId, String operationType, String targetType,
                                       Long targetId, String remark, String ip) {
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(adminId);
        log.setOperationType(operationType);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setRemark(remark);
        log.setIp(ip);
        return log;
    }

    private AdminLogResponse toLogResponse(AdminOperationLog log) {
        return new AdminLogResponse(
                log.getId(),
                log.getAdminId(),
                log.getOperationType(),
                log.getTargetType(),
                log.getTargetId(),
                log.getRemark(),
                log.getIp(),
                log.getCreateTime());
    }

    private void ensureOk(ApiResponse<?> resp) {
        if (resp == null || resp.code() != ErrorCode.SUCCESS.code()) {
            throw new BusinessException(ErrorCode.REMOTE_CALL_FAILED,
                    resp == null ? "远程服务调用失败" : resp.message());
        }
    }
}
