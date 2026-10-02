package com.share.rental.item.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员审核物品列表项响应。
 *
 * <p>管理员审核物品时需要看到普通用户会看到的完整内容，因此本 DTO
 * 在原有审核状态字段基础上，补充描述、标签、分类、价格、押金、数量、
 * 计价方式以及图片 URL 列表，避免管理端只能看到标题。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemAuditResponse {
    private Long id;
    private Long ownerId;
    private String title;
    private Integer status;
    private Integer auditStatus;
    private String auditReason;
    private LocalDateTime auditTime;
    private Long auditAdminId;

    // ===== 扩展字段：物品完整内容 =====
    private String description;
    private Long categoryId;
    /** 分类名称，便于审核员阅读，无需再二次查询 */
    private String categoryName;
    private String tags;
    /** 计价方式：0=免费 1=按天 */
    private Integer priceType;
    private BigDecimal dailyPrice;
    private BigDecimal depositAmount;
    private Integer quantity;
    /** 物品图片 URL 列表，按 sort_order 升序 */
    private List<String> imageUrls;
}
