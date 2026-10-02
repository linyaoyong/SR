package com.share.rental.item.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.item.dto.ItemAdminStatsResponse;
import com.share.rental.item.dto.ItemAuditActionResponse;
import com.share.rental.item.dto.ItemAuditResponse;
import com.share.rental.item.dto.ItemInfoFeignResponse;
import com.share.rental.item.dto.ItemSnapshotFeignResponse;
import com.share.rental.item.entity.Category;
import com.share.rental.item.entity.Item;
import com.share.rental.item.entity.ItemImage;
import com.share.rental.item.entity.ItemSnapshot;
import com.share.rental.item.enums.ItemStatusEnum;
import com.share.rental.item.mapper.CategoryMapper;
import com.share.rental.item.mapper.ItemImageMapper;
import com.share.rental.item.mapper.ItemMapper;
import com.share.rental.item.mapper.ItemSnapshotMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemInternalService {

    private static final int AUDIT_APPROVED = 1;
    private static final int AUDIT_RECTIFY = 2;

    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final ItemSnapshotMapper itemSnapshotMapper;
    private final CategoryMapper categoryMapper;

    @Value("${resilience.demo.item-delay-ms:0}")
    private long itemDelayMs;

    @Autowired
    public ItemInternalService(ItemMapper itemMapper,
                                ItemImageMapper itemImageMapper,
                                ItemSnapshotMapper itemSnapshotMapper,
                                CategoryMapper categoryMapper) {
        this.itemMapper = itemMapper;
        this.itemImageMapper = itemImageMapper;
        this.itemSnapshotMapper = itemSnapshotMapper;
        this.categoryMapper = categoryMapper;
    }

    public ItemInfoFeignResponse getItemInfo(Long id) {
        applyDemoDelay();
        Item item = loadItemOrThrow(id);
        ItemImage firstImage = itemImageMapper.selectOne(new LambdaQueryWrapper<ItemImage>()
                .eq(ItemImage::getItemId, id)
                .orderByAsc(ItemImage::getSortOrder)
                .orderByAsc(ItemImage::getId)
                .last("LIMIT 1"));
        String firstImageUrl = firstImage == null ? null : firstImage.getUrl();
        return new ItemInfoFeignResponse(
                item.getId(),
                item.getOwnerId(),
                item.getTitle(),
                item.getQuantity(),
                item.getRentedCount(),
                item.getDailyPrice(),
                item.getDepositAmount(),
                item.getMinRentDays(),
                item.getStatus(),
                item.getAuditStatus(),
                firstImageUrl
        );
    }

    public Long createSnapshot(Long itemId) {
        applyDemoDelay();
        Item item = loadItemOrThrow(itemId);
        String categoryName = resolveCategoryName(item.getCategoryId());
        List<ItemImage> images = itemImageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                .eq(ItemImage::getItemId, itemId)
                .orderByAsc(ItemImage::getSortOrder));
        String imageUrls = images == null || images.isEmpty()
                ? null
                : images.stream().map(ItemImage::getUrl).collect(Collectors.joining(","));
        ItemSnapshot snapshot = new ItemSnapshot();
        snapshot.setItemId(itemId);
        snapshot.setOwnerId(item.getOwnerId());
        snapshot.setTitle(item.getTitle());
        snapshot.setDescription(item.getDescription());
        snapshot.setCategoryName(categoryName);
        snapshot.setImageUrls(imageUrls);
        snapshot.setPriceType(item.getPriceType());
        snapshot.setDailyPrice(item.getDailyPrice());
        snapshot.setDepositAmount(item.getDepositAmount());
        snapshot.setSupportDelivery(item.getSupportDelivery());
        snapshot.setSupportMeetup(item.getSupportMeetup());
        snapshot.setSnapshotJson(buildSnapshotJson(item));
        itemSnapshotMapper.insert(snapshot);
        return snapshot.getId();
    }

    public ItemSnapshotFeignResponse getSnapshot(Long snapshotId) {
        applyDemoDelay();
        ItemSnapshot snapshot = itemSnapshotMapper.selectById(snapshotId);
        if (snapshot == null) {
            throw new BusinessException(ErrorCode.ITEM_NOT_FOUND);
        }
        return new ItemSnapshotFeignResponse(
                snapshot.getId(),
                snapshot.getItemId(),
                snapshot.getOwnerId(),
                snapshot.getTitle(),
                snapshot.getDescription(),
                snapshot.getCategoryName(),
                snapshot.getImageUrls(),
                snapshot.getPriceType(),
                snapshot.getDailyPrice(),
                snapshot.getDepositAmount(),
                snapshot.getSupportDelivery(),
                snapshot.getSupportMeetup()
        );
    }

    public ItemAdminStatsResponse getAdminStats() {
        long totalItems = itemMapper.selectCount(null);
        return new ItemAdminStatsResponse(totalItems);
    }

    public List<ItemAuditResponse> listAuditItems(Integer auditStatus) {
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<>();
        if (auditStatus != null) {
            wrapper.eq(Item::getAuditStatus, auditStatus);
        }
        wrapper.orderByDesc(Item::getAuditTime);
        List<Item> items = itemMapper.selectList(wrapper);
        if (items.isEmpty()) {
            return List.of();
        }
        // 批量查询图片，避免 N+1
        List<Long> itemIds = items.stream().map(Item::getId).collect(Collectors.toList());
        List<ItemImage> allImages = itemImageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                .in(ItemImage::getItemId, itemIds)
                .orderByAsc(ItemImage::getSortOrder));
        java.util.Map<Long, List<String>> imageMap = allImages.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ItemImage::getItemId,
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.mapping(ItemImage::getUrl, java.util.stream.Collectors.toList())));
        return items.stream()
                .map(item -> new ItemAuditResponse(
                        item.getId(),
                        item.getOwnerId(),
                        item.getTitle(),
                        item.getStatus(),
                        item.getAuditStatus(),
                        item.getAuditReason(),
                        item.getAuditTime(),
                        item.getAuditAdminId(),
                        item.getDescription(),
                        item.getCategoryId(),
                        resolveCategoryName(item.getCategoryId()),
                        item.getTags(),
                        item.getPriceType(),
                        item.getDailyPrice(),
                        item.getDepositAmount(),
                        item.getQuantity(),
                        imageMap.getOrDefault(item.getId(), List.of())
                ))
                .collect(Collectors.toList());
    }

    public ItemAuditActionResponse auditItem(Long itemId, Integer auditStatus, String reason, Long adminId) {
        if (auditStatus == null || (auditStatus != AUDIT_APPROVED && auditStatus != AUDIT_RECTIFY)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        if (auditStatus == AUDIT_RECTIFY && (reason == null || reason.isBlank())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        Item item = loadItemOrThrow(itemId);
        item.setAuditStatus(auditStatus);
        item.setAuditReason(reason);
        item.setAuditTime(LocalDateTime.now());
        item.setAuditAdminId(adminId);
        itemMapper.updateById(item);
        return new ItemAuditActionResponse(
                item.getId(),
                item.getOwnerId(),
                item.getAuditStatus(),
                item.getAuditReason(),
                item.getAuditTime(),
                item.getAuditAdminId()
        );
    }

    public void forceOffShelf(Long itemId) {
        Item item = loadItemOrThrow(itemId);
        item.setStatus(ItemStatusEnum.FORCE_OFF_SHELF.code());
        // 与 docs/03-api-contract.md 描述对齐：强制下架同时把审核状态置为要求整改(2)
        item.setAuditStatus(AUDIT_RECTIFY);
        itemMapper.updateById(item);
    }

    public void reserveRentedCount(Long itemId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        Item item = loadItemOrThrow(itemId);
        Integer totalQuantity = item.getQuantity();
        int rentedCount = item.getRentedCount() == null ? 0 : item.getRentedCount();
        if (totalQuantity == null || rentedCount + quantity > totalQuantity) {
            throw new BusinessException(ErrorCode.RENTAL_ITEM_UNAVAILABLE);
        }
        item.setRentedCount(rentedCount + quantity);
        itemMapper.updateById(item);
    }

    public void releaseRentedCount(Long itemId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        Item item = loadItemOrThrow(itemId);
        int rentedCount = item.getRentedCount() == null ? 0 : item.getRentedCount();
        item.setRentedCount(Math.max(0, rentedCount - quantity));
        itemMapper.updateById(item);
    }

    private Item loadItemOrThrow(Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.ITEM_NOT_FOUND);
        }
        return item;
    }

    private void applyDemoDelay() {
        if (itemDelayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(itemDelayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SERVICE_BUSY);
        }
    }

    private String resolveCategoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryMapper.selectById(categoryId);
        return category == null ? null : category.getName();
    }

    private String buildSnapshotJson(Item item) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"itemId\":").append(item.getId()).append(",");
        sb.append("\"ownerId\":").append(item.getOwnerId()).append(",");
        sb.append("\"quantity\":").append(item.getQuantity()).append(",");
        sb.append("\"rentedCount\":").append(item.getRentedCount()).append(",");
        sb.append("\"status\":").append(item.getStatus()).append(",");
        sb.append("\"auditStatus\":").append(item.getAuditStatus());
        sb.append("}");
        return sb.toString();
    }
}
