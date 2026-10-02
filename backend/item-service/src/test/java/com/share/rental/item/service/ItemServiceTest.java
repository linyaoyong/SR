package com.share.rental.item.service;

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
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                Item.class);
    }

    @Mock
    private ItemMapper itemMapper;
    @Mock
    private ItemImageMapper itemImageMapper;
    @Mock
    private AuthFeignClient authClient;
    @Mock
    private WalletFeignClient walletClient;
    @Mock
    private ImageUploadService imageUploadService;

    @InjectMocks
    private ItemService itemService;

    @Test
    void createItem_bannedUser_rejected() {
        when(authClient.getUserStatus(10L)).thenReturn(
                ApiResponse.success(new UserStatusFeignResponse(10L, 1, 100, 0)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> itemService.create(10L, validCreateRequest()));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.ITEM_PUBLISH_FORBIDDEN);
    }

    @Test
    void createItem_walletNotUsable_rejected() {
        when(authClient.getUserStatus(10L)).thenReturn(
                ApiResponse.success(new UserStatusFeignResponse(10L, 0, 100, 0)));
        when(walletClient.isUsable(10L)).thenReturn(
                ApiResponse.success(new WalletUsableResponse(10L, false, BigDecimal.ZERO)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> itemService.create(10L, validCreateRequest()));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.ITEM_PUBLISH_FORBIDDEN);
    }

    @Test
    void createItem_success_setsDefaultStatusAndAuditAndRentedCount() {
        when(authClient.getUserStatus(10L)).thenReturn(
                ApiResponse.success(new UserStatusFeignResponse(10L, 0, 100, 0)));
        when(walletClient.isUsable(10L)).thenReturn(
                ApiResponse.success(new WalletUsableResponse(10L, true, new BigDecimal("88.50"))));
        when(itemMapper.insert(any(Item.class))).thenAnswer(inv -> {
            Item it = inv.getArgument(0);
            it.setId(100L);
            return 1;
        });
        lenient().when(itemImageMapper.selectList(any())).thenReturn(List.of());

        ItemDetailResponse resp = itemService.create(10L, validCreateRequest());

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).insert(captor.capture());
        Item saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ItemStatusEnum.LISTED.code());
        assertThat(saved.getAuditStatus()).isZero();
        assertThat(saved.getRentedCount()).isZero();
        assertThat(saved.getOwnerId()).isEqualTo(10L);
        assertThat(resp.getId()).isEqualTo(100L);
        assertThat(resp.getTitle()).isEqualTo("电动工具");
        assertThat(resp.getStatus()).isEqualTo(ItemStatusEnum.LISTED.code());
        assertThat(resp.getAuditStatus()).isZero();
    }

    @Test
    void listPublic_includesPendingAndHidesRectify() {
        Item pending = listedItem(1L, 10L, 0);
        when(itemMapper.selectList(any())).thenReturn(List.of(pending));
        lenient().when(itemMapper.selectCount(any())).thenReturn(1L);
        lenient().when(itemImageMapper.selectList(any())).thenReturn(List.of());

        PageResult<ItemListResponse> page = itemService.listPublic(null, null, 1, 20);

        assertThat(page.records()).extracting(ItemListResponse::getId).containsExactly(1L);
    }

    @Test
    void listPublic_keywordMatchesTitleDescriptionAndTags() {
        Item tagged = listedItem(1L, 10L, 1);
        tagged.setTitle("露营椅");
        tagged.setDescription("适合周末野餐");
        tagged.setTags("户外,折叠");
        when(itemMapper.selectList(any())).thenReturn(List.of(tagged));
        lenient().when(itemMapper.selectCount(any())).thenReturn(1L);
        lenient().when(itemImageMapper.selectList(any())).thenReturn(List.of());

        PageResult<ItemListResponse> page = itemService.listPublic(null, "折叠", 1, 20);

        assertThat(page.records()).extracting(ItemListResponse::getTitle).containsExactly("露营椅");
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Item>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(itemMapper).selectCount(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertThat(sqlSegment)
                .contains("title")
                .contains("description")
                .contains("tags")
                .contains("OR");
    }

    @Test
    void listMine_returnsOwnerItems() {
        Item mine = listedItem(1L, 10L, 0);
        when(itemMapper.selectList(any())).thenReturn(List.of(mine));
        lenient().when(itemMapper.selectCount(any())).thenReturn(1L);
        lenient().when(itemImageMapper.selectList(any())).thenReturn(List.of());

        PageResult<ItemListResponse> page = itemService.listMine(10L, 1, 20);

        assertThat(page.records()).extracting(ItemListResponse::getId).containsExactly(1L);
    }

    @Test
    void updateItem_resetsAuditStatusToPending() {
        Item item = listedItem(3L, 10L, 2);
        item.setAuditReason("图片不清晰");
        item.setAuditAdminId(7L);
        when(itemMapper.selectById(3L)).thenReturn(item);

        itemService.update(10L, 3L, validUpdateRequest());

        assertThat(item.getAuditStatus()).isZero();
        assertThat(item.getAuditReason()).isNull();
        assertThat(item.getAuditAdminId()).isNull();
        verify(itemMapper).updateById(item);
    }

    @Test
    void updateItem_notOwner_rejected() {
        Item item = listedItem(3L, 10L, 0);
        when(itemMapper.selectById(3L)).thenReturn(item);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> itemService.update(99L, 3L, validUpdateRequest()));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.ITEM_OWNER_REQUIRED);
    }

    @Test
    void detail_publicCanViewListedAndAudited() {
        Item item = listedItem(1L, 10L, 1);
        when(itemMapper.selectById(1L)).thenReturn(item);
        lenient().when(itemImageMapper.selectList(any())).thenReturn(List.of());

        ItemDetailResponse resp = itemService.detail(99L, 1L);

        assertThat(resp.getId()).isEqualTo(1L);
    }

    @Test
    void detail_publicCannotViewRectified() {
        Item item = listedItem(1L, 10L, 2);
        when(itemMapper.selectById(1L)).thenReturn(item);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> itemService.detail(99L, 1L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.ITEM_OFF_SHELF);
    }

    @Test
    void detail_ownerCanViewOwnRectified() {
        Item item = listedItem(1L, 10L, 2);
        when(itemMapper.selectById(1L)).thenReturn(item);
        lenient().when(itemImageMapper.selectList(any())).thenReturn(List.of());

        ItemDetailResponse resp = itemService.detail(10L, 1L);

        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getAuditStatus()).isEqualTo(2);
    }

    @Test
    void detail_forceOffShelf_returnsItemOffShelf() {
        Item item = listedItem(1L, 10L, 0);
        item.setStatus(ItemStatusEnum.FORCE_OFF_SHELF.code());
        when(itemMapper.selectById(1L)).thenReturn(item);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> itemService.detail(10L, 1L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.ITEM_OFF_SHELF);
    }

    @Test
    void detail_notFound() {
        when(itemMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> itemService.detail(10L, 1L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.ITEM_NOT_FOUND);
    }

    @Test
    void offShelf_success_setsStatusOffShelf() {
        Item item = listedItem(1L, 10L, 0);
        when(itemMapper.selectById(1L)).thenReturn(item);

        itemService.offShelf(10L, 1L);

        assertThat(item.getStatus()).isEqualTo(ItemStatusEnum.OFF_SHELF.code());
        verify(itemMapper).updateById(item);
    }

    @Test
    void offShelf_notOwner_rejected() {
        Item item = listedItem(1L, 10L, 0);
        when(itemMapper.selectById(1L)).thenReturn(item);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> itemService.offShelf(99L, 1L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.ITEM_OWNER_REQUIRED);
    }

    @Test
    void reList_success_setsStatusListed() {
        Item item = listedItem(1L, 10L, 0);
        item.setStatus(ItemStatusEnum.OFF_SHELF.code());
        when(itemMapper.selectById(1L)).thenReturn(item);

        itemService.reList(10L, 1L);

        assertThat(item.getStatus()).isEqualTo(ItemStatusEnum.LISTED.code());
        verify(itemMapper).updateById(item);
    }

    @Test
    void delete_success_callsDeleteById() {
        Item item = listedItem(1L, 10L, 0);
        when(itemMapper.selectById(1L)).thenReturn(item);

        itemService.delete(10L, 1L);

        verify(itemMapper).deleteById(1L);
    }

    @Test
    void delete_notOwner_rejected() {
        Item item = listedItem(1L, 10L, 0);
        when(itemMapper.selectById(1L)).thenReturn(item);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> itemService.delete(99L, 1L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.ITEM_OWNER_REQUIRED);
    }

    @Test
    void addImages_tooMany_rejected() {
        Item item = listedItem(1L, 10L, 0);
        when(itemMapper.selectById(1L)).thenReturn(item);
        when(itemImageMapper.selectCount(any())).thenReturn(9L);

        MultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1});

        BusinessException ex = assertThrows(BusinessException.class,
                () -> itemService.addImages(10L, 1L, List.of(file)));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.ITEM_IMAGE_TOO_MANY);
    }

    @Test
    void addImages_success_insertsAndReturnsActiveImages() {
        Item item = listedItem(1L, 10L, 0);
        when(itemMapper.selectById(1L)).thenReturn(item);
        when(itemImageMapper.selectCount(any())).thenReturn(1L);
        when(imageUploadService.storeImage(any(MultipartFile.class), eq("items")))
                .thenReturn(new UploadedFile("/files/items/a.jpg", "a.jpg", "image/jpeg", 1024));
        when(itemImageMapper.selectList(any())).thenReturn(List.of(
                existingImage(10L, 1L, "/files/items/old.jpg", 0),
                existingImage(11L, 1L, "/files/items/a.jpg", 1)
        ));

        MultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1});

        List<ItemImageResponse> images = itemService.addImages(10L, 1L, List.of(file));

        verify(itemImageMapper).insert(any(ItemImage.class));
        assertThat(images).extracting(ItemImageResponse::getUrl)
                .containsExactly("/files/items/old.jpg", "/files/items/a.jpg");
    }

    private Item listedItem(Long id, Long ownerId, Integer auditStatus) {
        Item item = new Item();
        item.setId(id);
        item.setOwnerId(ownerId);
        item.setTitle("item-" + id);
        item.setCategoryId(1L);
        item.setQuantity(1);
        item.setDailyPrice(new BigDecimal("10.00"));
        item.setMinRentDays(1);
        item.setStatus(ItemStatusEnum.LISTED.code());
        item.setAuditStatus(auditStatus);
        return item;
    }

    private ItemImage existingImage(Long id, Long itemId, String url, Integer sortOrder) {
        ItemImage image = new ItemImage();
        image.setId(id);
        image.setItemId(itemId);
        image.setUrl(url);
        image.setSortOrder(sortOrder);
        return image;
    }

    private ItemCreateRequest validCreateRequest() {
        ItemCreateRequest req = new ItemCreateRequest();
        req.setTitle("电动工具");
        req.setDescription("九成新电钻一把");
        req.setCategoryId(1L);
        req.setQuantity(1);
        req.setDailyPrice(new BigDecimal("20.00"));
        req.setMinRentDays(1);
        return req;
    }

    private ItemUpdateRequest validUpdateRequest() {
        ItemUpdateRequest req = new ItemUpdateRequest();
        req.setTitle("更新标题");
        req.setDescription("更新描述内容");
        return req;
    }
}
