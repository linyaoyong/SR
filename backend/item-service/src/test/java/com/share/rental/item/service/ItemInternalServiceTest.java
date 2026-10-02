package com.share.rental.item.service;

import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemInternalServiceTest {

    @Mock
    private ItemMapper itemMapper;
    @Mock
    private ItemImageMapper itemImageMapper;
    @Mock
    private ItemSnapshotMapper itemSnapshotMapper;
    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private ItemInternalService itemInternalService;

    @Test
    void getInfo_existingItem_returnsInfo() {
        Item item = listedItem(1L, 10L);
        when(itemMapper.selectById(1L)).thenReturn(item);

        ItemInfoFeignResponse resp = itemInternalService.getItemInfo(1L);

        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getOwnerId()).isEqualTo(10L);
        assertThat(resp.getTitle()).isEqualTo("item-1");
        assertThat(resp.getQuantity()).isEqualTo(1);
        assertThat(resp.getRentedCount()).isZero();
        assertThat(resp.getDailyPrice()).isEqualByComparingTo("10.00");
        assertThat(resp.getDepositAmount()).isEqualByComparingTo("50.00");
        assertThat(resp.getMinRentDays()).isEqualTo(1);
        assertThat(resp.getStatus()).isEqualTo(ItemStatusEnum.LISTED.code());
        assertThat(resp.getAuditStatus()).isZero();
    }

    @Test
    void getInfo_withDemoDelay_waitsBeforeReturning() {
        ReflectionTestUtils.setField(itemInternalService, "itemDelayMs", 5L);
        Item item = listedItem(1L, 10L);
        when(itemMapper.selectById(1L)).thenReturn(item);

        long start = System.nanoTime();
        itemInternalService.getItemInfo(1L);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMillis).isGreaterThanOrEqualTo(5L);
    }

    @Test
    void getInfo_missingItem_throwsNotFound() {
        when(itemMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> itemInternalService.getItemInfo(1L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.ITEM_NOT_FOUND);
    }

    @Test
    void createSnapshot_writesSnapshot() {
        Item item = listedItem(1L, 10L);
        when(itemMapper.selectById(1L)).thenReturn(item);
        Category category = new Category();
        category.setId(1L);
        category.setName("工具");
        when(categoryMapper.selectById(1L)).thenReturn(category);
        ItemImage image = new ItemImage();
        image.setId(20L);
        image.setItemId(1L);
        image.setUrl("/files/items/a.jpg");
        image.setSortOrder(0);
        when(itemImageMapper.selectList(any())).thenReturn(List.of(image));

        itemInternalService.createSnapshot(1L);

        ArgumentCaptor<ItemSnapshot> captor = ArgumentCaptor.forClass(ItemSnapshot.class);
        verify(itemSnapshotMapper).insert(captor.capture());
        ItemSnapshot snap = captor.getValue();
        assertThat(snap.getItemId()).isEqualTo(1L);
        assertThat(snap.getOwnerId()).isEqualTo(10L);
        assertThat(snap.getTitle()).isEqualTo("item-1");
        assertThat(snap.getCategoryName()).isEqualTo("工具");
        assertThat(snap.getImageUrls()).isEqualTo("/files/items/a.jpg");
        assertThat(snap.getPriceType()).isEqualTo(1);
        assertThat(snap.getDailyPrice()).isEqualByComparingTo("10.00");
        assertThat(snap.getDepositAmount()).isEqualByComparingTo("50.00");
        assertThat(snap.getSupportDelivery()).isEqualTo(1);
        assertThat(snap.getSupportMeetup()).isEqualTo(0);
        assertThat(snap.getSnapshotJson()).contains("\"itemId\":1");
        assertThat(snap.getSnapshotJson()).contains("\"ownerId\":10");
    }

    @Test
    void getSnapshot_existingSnapshot_returnsImageUrls() {
        ItemSnapshot snapshot = new ItemSnapshot();
        snapshot.setId(99L);
        snapshot.setItemId(1L);
        snapshot.setOwnerId(10L);
        snapshot.setTitle("Switch 2");
        snapshot.setDescription("游戏机");
        snapshot.setCategoryName("数码");
        snapshot.setImageUrls("/files/items/a.jpg,/files/items/b.jpg");
        snapshot.setPriceType(1);
        snapshot.setDailyPrice(new BigDecimal("20.00"));
        snapshot.setDepositAmount(new BigDecimal("300.00"));
        snapshot.setSupportDelivery(1);
        snapshot.setSupportMeetup(1);
        when(itemSnapshotMapper.selectById(99L)).thenReturn(snapshot);

        ItemSnapshotFeignResponse resp = itemInternalService.getSnapshot(99L);

        assertThat(resp.getTitle()).isEqualTo("Switch 2");
        assertThat(resp.getCategoryName()).isEqualTo("数码");
        assertThat(resp.getImageUrls()).isEqualTo("/files/items/a.jpg,/files/items/b.jpg");
        assertThat(resp.getDailyPrice()).isEqualByComparingTo("20.00");
    }

    @Test
    void listAudits_byStatus_returnsList() {
        Item item = listedItem(1L, 10L);
        item.setAuditStatus(2);
        item.setAuditReason("图片不清晰");
        item.setAuditAdminId(7L);
        item.setAuditTime(LocalDateTime.of(2026, 6, 27, 12, 0));
        when(itemMapper.selectList(any())).thenReturn(List.of(item));

        List<ItemAuditResponse> result = itemInternalService.listAuditItems(2);

        assertThat(result).hasSize(1);
        ItemAuditResponse resp = result.get(0);
        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getOwnerId()).isEqualTo(10L);
        assertThat(resp.getTitle()).isEqualTo("item-1");
        assertThat(resp.getStatus()).isEqualTo(ItemStatusEnum.LISTED.code());
        assertThat(resp.getAuditStatus()).isEqualTo(2);
        assertThat(resp.getAuditReason()).isEqualTo("图片不清晰");
        assertThat(resp.getAuditAdminId()).isEqualTo(7L);
    }

    @Test
    void auditItem_rectify_setsAuditFields() {
        Item item = listedItem(1L, 10L);
        when(itemMapper.selectById(1L)).thenReturn(item);

        ItemAuditActionResponse resp = itemInternalService.auditItem(1L, 2, "图片不清晰", 7L);

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).updateById(captor.capture());
        Item updated = captor.getValue();
        assertThat(updated.getAuditStatus()).isEqualTo(2);
        assertThat(updated.getAuditReason()).isEqualTo("图片不清晰");
        assertThat(updated.getAuditTime()).isNotNull();
        assertThat(updated.getAuditAdminId()).isEqualTo(7L);
        assertThat(resp.getItemId()).isEqualTo(1L);
        assertThat(resp.getOwnerId()).isEqualTo(10L);
        assertThat(resp.getAuditStatus()).isEqualTo(2);
        assertThat(resp.getAuditReason()).isEqualTo("图片不清晰");
        assertThat(resp.getAuditAdminId()).isEqualTo(7L);
    }

    @Test
    void auditItem_approve_setsAuditStatus() {
        Item item = listedItem(1L, 10L);
        when(itemMapper.selectById(1L)).thenReturn(item);

        ItemAuditActionResponse resp = itemInternalService.auditItem(1L, 1, null, 7L);

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).updateById(captor.capture());
        Item updated = captor.getValue();
        assertThat(updated.getAuditStatus()).isEqualTo(1);
        assertThat(updated.getAuditReason()).isNull();
        assertThat(updated.getAuditTime()).isNotNull();
        assertThat(updated.getAuditAdminId()).isEqualTo(7L);
        assertThat(resp.getItemId()).isEqualTo(1L);
        assertThat(resp.getAuditStatus()).isEqualTo(1);
    }

    @Test
    void forceOffShelf_setsStatus3() {
        Item item = listedItem(1L, 10L);
        when(itemMapper.selectById(1L)).thenReturn(item);

        itemInternalService.forceOffShelf(1L);

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).updateById(captor.capture());
        Item updated = captor.getValue();
        assertThat(updated.getStatus()).isEqualTo(ItemStatusEnum.FORCE_OFF_SHELF.code());
    }

    @Test
    void reserveRentedCount_whenAvailable_increasesRentedCount() {
        Item item = listedItem(1L, 10L);
        item.setQuantity(2);
        item.setRentedCount(1);
        when(itemMapper.selectById(1L)).thenReturn(item);

        itemInternalService.reserveRentedCount(1L, 1);

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).updateById(captor.capture());
        assertThat(captor.getValue().getRentedCount()).isEqualTo(2);
    }

    @Test
    void reserveRentedCount_whenInsufficient_throwsUnavailable() {
        Item item = listedItem(1L, 10L);
        item.setQuantity(1);
        item.setRentedCount(1);
        when(itemMapper.selectById(1L)).thenReturn(item);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> itemInternalService.reserveRentedCount(1L, 1));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_ITEM_UNAVAILABLE);
    }

    @Test
    void releaseRentedCount_neverDropsBelowZero() {
        Item item = listedItem(1L, 10L);
        item.setRentedCount(1);
        when(itemMapper.selectById(1L)).thenReturn(item);

        itemInternalService.releaseRentedCount(1L, 3);

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).updateById(captor.capture());
        assertThat(captor.getValue().getRentedCount()).isZero();
    }

    private Item listedItem(Long id, Long ownerId) {
        Item item = new Item();
        item.setId(id);
        item.setOwnerId(ownerId);
        item.setTitle("item-" + id);
        item.setCategoryId(1L);
        item.setQuantity(1);
        item.setRentedCount(0);
        item.setSupportDelivery(1);
        item.setSupportMeetup(0);
        item.setPriceType(1);
        item.setDailyPrice(new BigDecimal("10.00"));
        item.setDepositAmount(new BigDecimal("50.00"));
        item.setMinRentDays(1);
        item.setStatus(ItemStatusEnum.LISTED.code());
        item.setAuditStatus(0);
        return item;
    }
}
