package com.share.rental.item.service;

import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.item.dto.FavoriteResponse;
import com.share.rental.item.entity.Favorite;
import com.share.rental.item.entity.Item;
import com.share.rental.item.enums.ItemStatusEnum;
import com.share.rental.item.mapper.FavoriteMapper;
import com.share.rental.item.mapper.ItemImageMapper;
import com.share.rental.item.mapper.ItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteMapper favoriteMapper;
    @Mock
    private ItemMapper itemMapper;
    @Mock
    private ItemImageMapper itemImageMapper;

    @InjectMocks
    private FavoriteService favoriteService;

    @Test
    void favorite_existingInactiveRecord_reactivates() {
        Favorite old = new Favorite();
        old.setUserId(10L);
        old.setItemId(1L);
        old.setStatus(0);
        when(favoriteMapper.selectOne(any())).thenReturn(old);
        when(itemMapper.selectById(1L)).thenReturn(listedItem(1L, 10L));

        favoriteService.favorite(10L, 1L);

        assertThat(old.getStatus()).isEqualTo(1);
        verify(favoriteMapper).updateById(old);
    }

    @Test
    void favorite_existingInactiveRecord_itemOffShelf_rejected() {
        Favorite old = new Favorite();
        old.setUserId(10L);
        old.setItemId(1L);
        old.setStatus(0);
        when(favoriteMapper.selectOne(any())).thenReturn(old);
        Item item = listedItem(1L, 10L);
        item.setStatus(ItemStatusEnum.OFF_SHELF.code());
        when(itemMapper.selectById(1L)).thenReturn(item);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> favoriteService.favorite(10L, 1L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.ITEM_OFF_SHELF);
        verify(favoriteMapper, never()).updateById(any(Favorite.class));
    }

    @Test
    void favorite_existingActiveRecord_isNoop() {
        Favorite old = new Favorite();
        old.setUserId(10L);
        old.setItemId(1L);
        old.setStatus(1);
        when(favoriteMapper.selectOne(any())).thenReturn(old);

        favoriteService.favorite(10L, 1L);

        verify(favoriteMapper, never()).updateById(any(Favorite.class));
        verify(favoriteMapper, never()).insert(any(Favorite.class));
    }

    @Test
    void favorite_noExistingRecord_validatesItemAndInserts() {
        Item item = listedItem(1L, 10L);
        when(favoriteMapper.selectOne(any())).thenReturn(null);
        when(itemMapper.selectById(1L)).thenReturn(item);

        favoriteService.favorite(20L, 1L);

        ArgumentCaptor<Favorite> captor = ArgumentCaptor.forClass(Favorite.class);
        verify(favoriteMapper).insert(captor.capture());
        Favorite saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(20L);
        assertThat(saved.getItemId()).isEqualTo(1L);
        assertThat(saved.getStatus()).isEqualTo(1);
    }

    @Test
    void favorite_itemNotFound_rejected() {
        when(favoriteMapper.selectOne(any())).thenReturn(null);
        when(itemMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> favoriteService.favorite(20L, 1L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.ITEM_NOT_FOUND);
        verify(favoriteMapper, never()).insert(any(Favorite.class));
    }

    @Test
    void favorite_itemOffShelf_rejected() {
        Item item = listedItem(1L, 10L);
        item.setStatus(ItemStatusEnum.OFF_SHELF.code());
        when(favoriteMapper.selectOne(any())).thenReturn(null);
        when(itemMapper.selectById(1L)).thenReturn(item);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> favoriteService.favorite(20L, 1L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.ITEM_OFF_SHELF);
    }

    @Test
    void favorite_itemRectifyAuditStatus_rejected() {
        Item item = listedItem(1L, 10L);
        item.setAuditStatus(2);
        when(favoriteMapper.selectOne(any())).thenReturn(null);
        when(itemMapper.selectById(1L)).thenReturn(item);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> favoriteService.favorite(20L, 1L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.ITEM_OFF_SHELF);
    }

    @Test
    void unfavorite_missingRecord_isNoop() {
        when(favoriteMapper.selectOne(any())).thenReturn(null);

        favoriteService.unfavorite(10L, 1L);

        verify(favoriteMapper, never()).updateById(any(Favorite.class));
    }

    @Test
    void unfavorite_existingActiveRecord_deactivates() {
        Favorite old = new Favorite();
        old.setUserId(10L);
        old.setItemId(1L);
        old.setStatus(1);
        when(favoriteMapper.selectOne(any())).thenReturn(old);

        favoriteService.unfavorite(10L, 1L);

        assertThat(old.getStatus()).isEqualTo(0);
        verify(favoriteMapper).updateById(old);
    }

    @Test
    void unfavorite_existingInactiveRecord_isNoop() {
        Favorite old = new Favorite();
        old.setUserId(10L);
        old.setItemId(1L);
        old.setStatus(0);
        when(favoriteMapper.selectOne(any())).thenReturn(old);

        favoriteService.unfavorite(10L, 1L);

        verify(favoriteMapper, never()).updateById(any(Favorite.class));
    }

    @Test
    void list_returnsActiveFavoritesWithItemInfo() {
        Favorite fav = new Favorite();
        fav.setId(5L);
        fav.setUserId(10L);
        fav.setItemId(1L);
        fav.setStatus(1);
        when(favoriteMapper.selectList(any())).thenReturn(List.of(fav));
        when(itemMapper.selectBatchIds(anyList())).thenReturn(List.of(listedItem(1L, 10L)));
        when(itemImageMapper.selectList(any())).thenReturn(List.of());

        List<FavoriteResponse> result = favoriteService.list(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemId()).isEqualTo(1L);
        assertThat(result.get(0).getItemTitle()).isEqualTo("item-1");
    }

    private Item listedItem(Long id, Long ownerId) {
        Item item = new Item();
        item.setId(id);
        item.setOwnerId(ownerId);
        item.setTitle("item-" + id);
        item.setQuantity(1);
        item.setDailyPrice(new BigDecimal("10.00"));
        item.setStatus(ItemStatusEnum.LISTED.code());
        item.setAuditStatus(0);
        return item;
    }
}
