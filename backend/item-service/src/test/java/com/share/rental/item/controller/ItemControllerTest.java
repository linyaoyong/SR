package com.share.rental.item.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.exception.GlobalExceptionHandler;
import com.share.rental.common.response.PageResult;
import com.share.rental.item.dto.ItemCreateRequest;
import com.share.rental.item.dto.ItemDetailResponse;
import com.share.rental.item.dto.ItemImageResponse;
import com.share.rental.item.dto.ItemListResponse;
import com.share.rental.item.dto.ItemUpdateRequest;
import com.share.rental.item.service.ItemService;
import com.share.rental.common.upload.UploadProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(UploadProperties.class)
class ItemControllerTest {

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
    private ItemService itemService;

    @Test
    void list_returnsPage() throws Exception {
        PageResult<ItemListResponse> page = PageResult.of(
                List.of(new ItemListResponse(1L, "电钻", 1L, new BigDecimal("20.00"), 1,
                        null, 1, 0, null, LocalDateTime.now())),
                1L, 1, 20);
        when(itemService.listPublic(eq(1L), eq("电"), eq(1), eq(20))).thenReturn(page);

        mvc.perform(get("/api/items")
                        .param("categoryId", "1")
                        .param("keyword", "电")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].id").value(1))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void mine_returnsOwnerPage() throws Exception {
        PageResult<ItemListResponse> page = PageResult.of(List.of(), 0L, 1, 20);
        when(itemService.listMine(eq(10L), eq(1), eq(20))).thenReturn(page);

        mvc.perform(get("/api/items/mine")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void detail_returnsDetail() throws Exception {
        when(itemService.detail(eq(10L), eq(1L))).thenReturn(detailResponse(1L));

        mvc.perform(get("/api/items/1")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void detail_notFound_returnsItemNotFoundCode() throws Exception {
        doThrow(new BusinessException(ErrorCode.ITEM_NOT_FOUND))
                .when(itemService).detail(any(), eq(1L));

        mvc.perform(get("/api/items/1")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40200));
    }

    @Test
    void create_validBody_returnsCreated() throws Exception {
        when(itemService.create(eq(10L), any(ItemCreateRequest.class))).thenReturn(detailResponse(100L));

        mvc.perform(post("/api/items")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(100));
    }

    @Test
    void create_invalidBody_returnsValidationError() throws Exception {
        ItemCreateRequest req = new ItemCreateRequest();

        mvc.perform(post("/api/items")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40004));
    }

    @Test
    void update_validBody_returnsSuccess() throws Exception {
        doNothing().when(itemService).update(eq(10L), eq(1L), any(ItemUpdateRequest.class));

        mvc.perform(put("/api/items/1")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ItemUpdateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void offShelf_returnsSuccess() throws Exception {
        doNothing().when(itemService).offShelf(eq(10L), eq(1L));

        mvc.perform(put("/api/items/1/off-shelf")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void reList_returnsSuccess() throws Exception {
        doNothing().when(itemService).reList(eq(10L), eq(1L));

        mvc.perform(put("/api/items/1/re-list")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void delete_returnsSuccess() throws Exception {
        doNothing().when(itemService).delete(eq(10L), eq(1L));

        mvc.perform(delete("/api/items/1")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void addImages_returnsImageList() throws Exception {
        when(itemService.addImages(eq(10L), eq(1L), any())).thenReturn(List.of(
                new ItemImageResponse(1L, "/files/items/a.jpg", 0)
        ));

        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1});

        mvc.perform(multipart("/api/items/1/images")
                        .file(file)
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].url").value("/files/items/a.jpg"));
    }

    private ItemDetailResponse detailResponse(Long id) {
        return new ItemDetailResponse(
                id, 10L, "电钻", "九成新", 1L, null, 1, 0,
                0, null, 0, null, 1, new BigDecimal("20.00"), 1, null,
                0, null, 0, null, null, null, null, 1, 0, null,
                List.of(), LocalDateTime.now());
    }

    private ItemCreateRequest validCreateRequest() {
        ItemCreateRequest req = new ItemCreateRequest();
        req.setTitle("电钻");
        req.setDescription("九成新电钻一把");
        req.setCategoryId(1L);
        req.setQuantity(1);
        req.setDailyPrice(new BigDecimal("20.00"));
        req.setMinRentDays(1);
        return req;
    }
}
