package com.share.rental.item.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.item.dto.FavoriteResponse;
import com.share.rental.item.entity.Favorite;
import com.share.rental.item.entity.Item;
import com.share.rental.item.entity.ItemImage;
import com.share.rental.item.enums.ItemStatusEnum;
import com.share.rental.item.mapper.FavoriteMapper;
import com.share.rental.item.mapper.ItemImageMapper;
import com.share.rental.item.mapper.ItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_CANCELED = 0;

    private final FavoriteMapper favoriteMapper;
    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;

    @Autowired
    public FavoriteService(FavoriteMapper favoriteMapper,
                           ItemMapper itemMapper,
                           ItemImageMapper itemImageMapper) {
        this.favoriteMapper = favoriteMapper;
        this.itemMapper = itemMapper;
        this.itemImageMapper = itemImageMapper;
    }

    public void favorite(Long userId, Long itemId) {
        Favorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getItemId, itemId));
        if (existing != null) {
            if (existing.getStatus() != null && existing.getStatus() == STATUS_ACTIVE) {
                return;
            }
            validatePublicVisible(itemId);
            existing.setStatus(STATUS_ACTIVE);
            favoriteMapper.updateById(existing);
            return;
        }
        validatePublicVisible(itemId);
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setItemId(itemId);
        favorite.setStatus(STATUS_ACTIVE);
        favoriteMapper.insert(favorite);
    }

    public void unfavorite(Long userId, Long itemId) {
        Favorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getItemId, itemId));
        if (existing == null) {
            return;
        }
        if (existing.getStatus() != null && existing.getStatus() == STATUS_CANCELED) {
            return;
        }
        existing.setStatus(STATUS_CANCELED);
        favoriteMapper.updateById(existing);
    }

    public List<FavoriteResponse> list(Long userId) {
        List<Favorite> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getStatus, STATUS_ACTIVE)
                .orderByDesc(Favorite::getCreateTime));
        if (favorites == null || favorites.isEmpty()) {
            return List.of();
        }
        List<Long> itemIds = favorites.stream().map(Favorite::getItemId).collect(Collectors.toList());
        List<Item> items = itemMapper.selectBatchIds(itemIds);
        Map<Long, Item> itemMap = items == null ? Map.of()
                : items.stream().collect(Collectors.toMap(Item::getId, i -> i));
        List<ItemImage> images = itemImageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                .in(ItemImage::getItemId, itemIds)
                .orderByAsc(ItemImage::getSortOrder));
        Map<Long, String> firstImage = new HashMap<>();
        if (images != null) {
            for (ItemImage image : images) {
                firstImage.putIfAbsent(image.getItemId(), image.getUrl());
            }
        }
        return favorites.stream()
                .map(fav -> {
                    Item item = itemMap.get(fav.getItemId());
                    if (item == null) {
                        return null;
                    }
                    return new FavoriteResponse(
                            fav.getId(),
                            fav.getItemId(),
                            item.getTitle(),
                            item.getDailyPrice(),
                            firstImage.get(fav.getItemId()),
                            fav.getCreateTime()
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void validatePublicVisible(Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.ITEM_NOT_FOUND);
        }
        if (item.getStatus() == null || item.getStatus() != ItemStatusEnum.LISTED.code()) {
            throw new BusinessException(ErrorCode.ITEM_OFF_SHELF);
        }
        if (item.getAuditStatus() != null && item.getAuditStatus() == 2) {
            throw new BusinessException(ErrorCode.ITEM_OFF_SHELF);
        }
    }
}
