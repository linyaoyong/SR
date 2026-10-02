package com.share.rental.item.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.response.ApiResponse;
import com.share.rental.common.response.PageResult;
import com.share.rental.common.upload.ImageUploadService;
import com.share.rental.common.upload.UploadedFile;
import com.share.rental.item.client.AuthFeignClient;
import com.share.rental.item.client.WalletFeignClient;
import com.share.rental.item.dto.ItemCreateRequest;
import com.share.rental.item.dto.ItemDetailResponse;
import com.share.rental.item.dto.ItemImageResponse;
import com.share.rental.item.dto.ItemListResponse;
import com.share.rental.item.dto.ItemUpdateRequest;
import com.share.rental.item.dto.UserStatusFeignResponse;
import com.share.rental.item.dto.WalletUsableResponse;
import com.share.rental.item.entity.Item;
import com.share.rental.item.entity.ItemImage;
import com.share.rental.item.enums.ItemStatusEnum;
import com.share.rental.item.mapper.ItemImageMapper;
import com.share.rental.item.mapper.ItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ItemService {

    private static final int MAX_IMAGES = 9;
    private static final String IMAGE_BUCKET = "items";

    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final AuthFeignClient authClient;
    private final WalletFeignClient walletClient;
    private final ImageUploadService imageUploadService;

    @Autowired
    public ItemService(ItemMapper itemMapper,
                       ItemImageMapper itemImageMapper,
                       AuthFeignClient authClient,
                       WalletFeignClient walletClient,
                       ImageUploadService imageUploadService) {
        this.itemMapper = itemMapper;
        this.itemImageMapper = itemImageMapper;
        this.authClient = authClient;
        this.walletClient = walletClient;
        this.imageUploadService = imageUploadService;
    }

    public ItemDetailResponse create(Long ownerId, ItemCreateRequest request) {
        validatePublishAllowed(ownerId);
        Item item = new Item();
        item.setOwnerId(ownerId);
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setCategoryId(request.getCategoryId());
        item.setTags(request.getTags());
        item.setQuantity(request.getQuantity());
        item.setRentedCount(0);
        item.setSupportDelivery(request.getSupportDelivery());
        item.setDeliveryCity(request.getDeliveryCity());
        item.setSupportMeetup(request.getSupportMeetup());
        item.setMeetupLocation(request.getMeetupLocation());
        item.setPriceType(request.getPriceType());
        item.setDailyPrice(request.getDailyPrice());
        item.setMinRentDays(request.getMinRentDays());
        item.setFreeRent(request.getFreeRent());
        item.setDepositEnabled(request.getDepositEnabled());
        item.setDepositAmount(request.getDepositAmount());
        item.setCreditDepositEnabled(request.getCreditDepositEnabled());
        item.setMinCreditScore(request.getMinCreditScore());
        item.setFreeDepositScore(request.getFreeDepositScore());
        item.setReducedDepositScore(request.getReducedDepositScore());
        item.setReducedDepositAmount(request.getReducedDepositAmount());
        item.setStatus(ItemStatusEnum.LISTED.code());
        item.setAuditStatus(0);
        itemMapper.insert(item);
        return toDetailResponse(item);
    }

    public void update(Long ownerId, Long itemId, ItemUpdateRequest request) {
        Item item = loadAndCheckOwner(ownerId, itemId);
        if (request.getTitle() != null) {
            item.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        if (request.getCategoryId() != null) {
            item.setCategoryId(request.getCategoryId());
        }
        if (request.getTags() != null) {
            item.setTags(request.getTags());
        }
        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
        }
        if (request.getSupportDelivery() != null) {
            item.setSupportDelivery(request.getSupportDelivery());
        }
        if (request.getDeliveryCity() != null) {
            item.setDeliveryCity(request.getDeliveryCity());
        }
        if (request.getSupportMeetup() != null) {
            item.setSupportMeetup(request.getSupportMeetup());
        }
        if (request.getMeetupLocation() != null) {
            item.setMeetupLocation(request.getMeetupLocation());
        }
        if (request.getPriceType() != null) {
            item.setPriceType(request.getPriceType());
        }
        if (request.getDailyPrice() != null) {
            item.setDailyPrice(request.getDailyPrice());
        }
        if (request.getMinRentDays() != null) {
            item.setMinRentDays(request.getMinRentDays());
        }
        if (request.getFreeRent() != null) {
            item.setFreeRent(request.getFreeRent());
        }
        if (request.getDepositEnabled() != null) {
            item.setDepositEnabled(request.getDepositEnabled());
        }
        if (request.getDepositAmount() != null) {
            item.setDepositAmount(request.getDepositAmount());
        }
        if (request.getCreditDepositEnabled() != null) {
            item.setCreditDepositEnabled(request.getCreditDepositEnabled());
        }
        if (request.getMinCreditScore() != null) {
            item.setMinCreditScore(request.getMinCreditScore());
        }
        if (request.getFreeDepositScore() != null) {
            item.setFreeDepositScore(request.getFreeDepositScore());
        }
        if (request.getReducedDepositScore() != null) {
            item.setReducedDepositScore(request.getReducedDepositScore());
        }
        if (request.getReducedDepositAmount() != null) {
            item.setReducedDepositAmount(request.getReducedDepositAmount());
        }
        item.setAuditStatus(0);
        item.setAuditReason(null);
        item.setAuditTime(null);
        item.setAuditAdminId(null);
        itemMapper.updateById(item);
    }

    public PageResult<ItemListResponse> listPublic(Long categoryId, String keyword, int page, int size) {
        int pageNo = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<Item>()
                .eq(Item::getStatus, ItemStatusEnum.LISTED.code())
                .in(Item::getAuditStatus, 0, 1);
        if (categoryId != null) {
            wrapper.eq(Item::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Item::getTitle, kw)
                    .or()
                    .like(Item::getDescription, kw)
                    .or()
                    .like(Item::getTags, kw));
        }
        long total = itemMapper.selectCount(wrapper);
        int offset = (pageNo - 1) * pageSize;
        wrapper.orderByDesc(Item::getCreateTime)
                .last("LIMIT " + pageSize + " OFFSET " + offset);
        List<Item> items = itemMapper.selectList(wrapper);
        return PageResult.of(toListResponse(items), total, pageNo, pageSize);
    }

    public PageResult<ItemListResponse> listMine(Long ownerId, int page, int size) {
        int pageNo = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<Item>()
                .eq(Item::getOwnerId, ownerId);
        long total = itemMapper.selectCount(wrapper);
        int offset = (pageNo - 1) * pageSize;
        wrapper.orderByDesc(Item::getCreateTime)
                .last("LIMIT " + pageSize + " OFFSET " + offset);
        List<Item> items = itemMapper.selectList(wrapper);
        return PageResult.of(toListResponse(items), total, pageNo, pageSize);
    }

    public ItemDetailResponse detail(Long userId, Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.ITEM_NOT_FOUND);
        }
        if (item.getStatus() != null && item.getStatus() == ItemStatusEnum.FORCE_OFF_SHELF.code()) {
            throw new BusinessException(ErrorCode.ITEM_OFF_SHELF);
        }
        boolean isOwner = userId != null && userId.equals(item.getOwnerId());
        if (!isOwner) {
            if (item.getStatus() == null || item.getStatus() != ItemStatusEnum.LISTED.code()) {
                throw new BusinessException(ErrorCode.ITEM_OFF_SHELF);
            }
            if (item.getAuditStatus() != null && item.getAuditStatus() == 2) {
                throw new BusinessException(ErrorCode.ITEM_OFF_SHELF);
            }
        }
        return toDetailResponse(item);
    }

    public void offShelf(Long ownerId, Long itemId) {
        Item item = loadAndCheckOwner(ownerId, itemId);
        item.setStatus(ItemStatusEnum.OFF_SHELF.code());
        itemMapper.updateById(item);
    }

    public void reList(Long ownerId, Long itemId) {
        Item item = loadAndCheckOwner(ownerId, itemId);
        item.setStatus(ItemStatusEnum.LISTED.code());
        itemMapper.updateById(item);
    }

    public void delete(Long ownerId, Long itemId) {
        loadAndCheckOwner(ownerId, itemId);
        itemMapper.deleteById(itemId);
    }

    public List<ItemImageResponse> addImages(Long ownerId, Long itemId, List<MultipartFile> files) {
        loadAndCheckOwner(ownerId, itemId);
        long currentCount = countActiveImages(itemId);
        if (currentCount + files.size() > MAX_IMAGES) {
            throw new BusinessException(ErrorCode.ITEM_IMAGE_TOO_MANY);
        }
        int sortOrder = (int) currentCount;
        for (MultipartFile file : files) {
            UploadedFile uploaded = imageUploadService.storeImage(file, IMAGE_BUCKET);
            ItemImage image = new ItemImage();
            image.setItemId(itemId);
            image.setUrl(uploaded.url());
            image.setSortOrder(sortOrder);
            itemImageMapper.insert(image);
            sortOrder++;
        }
        return loadActiveImages(itemId);
    }

    private void validatePublishAllowed(Long ownerId) {
        ApiResponse<UserStatusFeignResponse> statusResp = authClient.getUserStatus(ownerId);
        UserStatusFeignResponse status = statusResp == null ? null : statusResp.data();
        if (status == null || status.getStatus() == null || status.getStatus() == 1) {
            throw new BusinessException(ErrorCode.ITEM_PUBLISH_FORBIDDEN);
        }
        ApiResponse<WalletUsableResponse> walletResp = walletClient.isUsable(ownerId);
        WalletUsableResponse wallet = walletResp == null ? null : walletResp.data();
        if (wallet == null || !Boolean.TRUE.equals(wallet.getUsable())) {
            throw new BusinessException(ErrorCode.ITEM_PUBLISH_FORBIDDEN);
        }
    }

    private Item loadAndCheckOwner(Long ownerId, Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.ITEM_NOT_FOUND);
        }
        if (!ownerId.equals(item.getOwnerId())) {
            throw new BusinessException(ErrorCode.ITEM_OWNER_REQUIRED);
        }
        return item;
    }

    private long countActiveImages(Long itemId) {
        Long count = itemImageMapper.selectCount(new LambdaQueryWrapper<ItemImage>()
                .eq(ItemImage::getItemId, itemId));
        return count == null ? 0L : count;
    }

    private List<ItemImageResponse> loadActiveImages(Long itemId) {
        List<ItemImage> images = itemImageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                .eq(ItemImage::getItemId, itemId)
                .orderByAsc(ItemImage::getSortOrder));
        return images.stream()
                .map(img -> new ItemImageResponse(img.getId(), img.getUrl(), img.getSortOrder()))
                .collect(Collectors.toList());
    }

    private ItemDetailResponse toDetailResponse(Item item) {
        List<ItemImageResponse> images = loadActiveImages(item.getId());
        return new ItemDetailResponse(
                item.getId(),
                item.getOwnerId(),
                item.getTitle(),
                item.getDescription(),
                item.getCategoryId(),
                item.getTags(),
                item.getQuantity(),
                item.getRentedCount(),
                item.getSupportDelivery(),
                item.getDeliveryCity(),
                item.getSupportMeetup(),
                item.getMeetupLocation(),
                item.getPriceType(),
                item.getDailyPrice(),
                item.getMinRentDays(),
                item.getFreeRent(),
                item.getDepositEnabled(),
                item.getDepositAmount(),
                item.getCreditDepositEnabled(),
                item.getMinCreditScore(),
                item.getFreeDepositScore(),
                item.getReducedDepositScore(),
                item.getReducedDepositAmount(),
                item.getStatus(),
                item.getAuditStatus(),
                item.getAuditReason(),
                images,
                item.getCreateTime()
        );
    }

    private List<ItemListResponse> toListResponse(List<Item> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<Long> itemIds = items.stream().map(Item::getId).collect(Collectors.toList());
        List<ItemImage> images = itemImageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                .in(ItemImage::getItemId, itemIds)
                .orderByAsc(ItemImage::getSortOrder));
        Map<Long, String> firstImage = new HashMap<>();
        if (images != null) {
            for (ItemImage image : images) {
                firstImage.putIfAbsent(image.getItemId(), image.getUrl());
            }
        }
        return items.stream()
                .map(item -> new ItemListResponse(
                        item.getId(),
                        item.getTitle(),
                        item.getCategoryId(),
                        item.getDailyPrice(),
                        item.getMinRentDays(),
                        item.getDepositAmount(),
                        item.getStatus(),
                        item.getAuditStatus(),
                        firstImage.get(item.getId()),
                        item.getCreateTime()
                ))
                .collect(Collectors.toList());
    }
}
