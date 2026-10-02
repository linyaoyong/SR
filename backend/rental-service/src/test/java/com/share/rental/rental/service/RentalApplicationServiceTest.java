package com.share.rental.rental.service;

import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.response.ApiResponse;
import com.share.rental.rental.client.AuthRentalClient;
import com.share.rental.rental.client.ItemRentalClient;
import com.share.rental.rental.client.MessageRentalClient;
import com.share.rental.rental.client.WalletRentalClient;
import com.share.rental.rental.dto.CardMessageRequest;
import com.share.rental.rental.dto.ItemInfoFeignResponse;
import com.share.rental.rental.dto.RentalApplicationCreateRequest;
import com.share.rental.rental.dto.RentalApplicationDetailResponse;
import com.share.rental.rental.dto.RentalApplicationResponse;
import com.share.rental.rental.dto.RentalProposalUpdateRequest;
import com.share.rental.rental.dto.UserStatusFeignResponse;
import com.share.rental.rental.dto.WalletUsableResponse;
import com.share.rental.rental.entity.RentalApplication;
import com.share.rental.rental.entity.RentalProposal;
import com.share.rental.rental.enums.ApplicationStatusEnum;
import com.share.rental.rental.mapper.RentalApplicationMapper;
import com.share.rental.rental.mapper.RentalProposalMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalApplicationServiceTest {

    @Mock
    private RentalApplicationMapper applicationMapper;
    @Mock
    private RentalProposalMapper proposalMapper;
    @Mock
    private ItemRentalClient itemClient;
    @Mock
    private AuthRentalClient authClient;
    @Mock
    private WalletRentalClient walletClient;
    @Mock
    private RentalOrderService orderService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private MessageRentalClient messageRentalClient;

    @InjectMocks
    private RentalApplicationService service;

    @org.junit.jupiter.api.BeforeEach
    void setUpRedis() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    }

    @Test
    void createApplication_createsApplicationAndFirstProposal() {
        when(itemClient.getItemInfo(50L)).thenReturn(
                ApiResponse.success(new ItemInfoFeignResponse(50L, 20L, "电钻", 1, 0,
                        new BigDecimal("10.00"), new BigDecimal("100.00"), 1, 1, 0)));
        when(authClient.getUserStatus(10L)).thenReturn(
                ApiResponse.success(new UserStatusFeignResponse(10L, 0, 100, 0)));
        when(walletClient.isUsable(10L)).thenReturn(
                ApiResponse.success(new WalletUsableResponse(10L, true, new BigDecimal("88.50"))));
        when(applicationMapper.insert(any(RentalApplication.class))).thenAnswer(inv -> {
            RentalApplication app = inv.getArgument(0);
            app.setId(100L);
            return 1;
        });
        when(proposalMapper.insert(any(RentalProposal.class))).thenAnswer(inv -> {
            RentalProposal p = inv.getArgument(0);
            p.setId(200L);
            return 1;
        });
        lenient().when(applicationMapper.updateById(any(RentalApplication.class))).thenReturn(1);

        RentalApplicationDetailResponse resp = service.createApplication(10L, validCreateRequest());

        ArgumentCaptor<RentalApplication> appCaptor = ArgumentCaptor.forClass(RentalApplication.class);
        verify(applicationMapper).insert(appCaptor.capture());
        RentalApplication savedApp = appCaptor.getValue();
        assertThat(savedApp.getItemId()).isEqualTo(50L);
        assertThat(savedApp.getRenterId()).isEqualTo(10L);
        assertThat(savedApp.getOwnerId()).isEqualTo(20L);
        assertThat(savedApp.getStatus()).isEqualTo(ApplicationStatusEnum.NEGOTIATING.code());
        assertThat(savedApp.getRenterConfirmed()).isEqualTo(1);
        assertThat(savedApp.getOwnerConfirmed()).isEqualTo(0);
        assertThat(savedApp.getPrepaidRentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(savedApp.getPreFrozenDepositAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        ArgumentCaptor<RentalProposal> proposalCaptor = ArgumentCaptor.forClass(RentalProposal.class);
        verify(proposalMapper).insert(proposalCaptor.capture());
        RentalProposal savedProposal = proposalCaptor.getValue();
        assertThat(savedProposal.getApplicationId()).isEqualTo(100L);
        assertThat(savedProposal.getVersionNo()).isEqualTo(1);
        assertThat(savedProposal.getOperatorId()).isEqualTo(10L);
        assertThat(savedProposal.getQuantity()).isEqualTo(1);
        assertThat(savedProposal.getDeliveryType()).isEqualTo(1);
        assertThat(savedProposal.getRentAmount()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(savedProposal.getDepositAmount()).isEqualByComparingTo(new BigDecimal("100.00"));

        ArgumentCaptor<RentalApplication> updateCaptor = ArgumentCaptor.forClass(RentalApplication.class);
        verify(applicationMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getCurrentProposalId()).isEqualTo(200L);

        assertThat(resp.getId()).isEqualTo(100L);
        assertThat(resp.getStatus()).isEqualTo(ApplicationStatusEnum.NEGOTIATING.code());
        assertThat(resp.getRenterConfirmed()).isEqualTo(1);
        assertThat(resp.getOwnerConfirmed()).isEqualTo(0);
        assertThat(resp.getCurrentProposal()).isNotNull();
        assertThat(resp.getCurrentProposal().getId()).isEqualTo(200L);
        assertThat(resp.getCurrentProposal().getVersionNo()).isEqualTo(1);
        assertThat(resp.getItemTitle()).isEqualTo("电钻");
    }

    @Test
    void createApplication_withoutAmountsUsesItemPriceAndDeposit() {
        when(itemClient.getItemInfo(50L)).thenReturn(
                ApiResponse.success(new ItemInfoFeignResponse(50L, 20L, "电钻", 3, 0,
                        new BigDecimal("10.00"), new BigDecimal("100.00"), 1, 1, 0)));
        when(authClient.getUserStatus(10L)).thenReturn(
                ApiResponse.success(new UserStatusFeignResponse(10L, 0, 100, 0)));
        when(walletClient.isUsable(10L)).thenReturn(
                ApiResponse.success(new WalletUsableResponse(10L, true, new BigDecimal("88.50"))));
        when(applicationMapper.insert(any(RentalApplication.class))).thenAnswer(inv -> {
            RentalApplication app = inv.getArgument(0);
            app.setId(100L);
            return 1;
        });
        when(proposalMapper.insert(any(RentalProposal.class))).thenAnswer(inv -> {
            RentalProposal p = inv.getArgument(0);
            p.setId(200L);
            return 1;
        });
        lenient().when(applicationMapper.updateById(any(RentalApplication.class))).thenReturn(1);

        RentalApplicationCreateRequest request = validCreateRequest();
        request.setQuantity(2);
        request.setRentAmount(null);
        request.setDepositAmount(null);

        service.createApplication(10L, request);

        ArgumentCaptor<RentalProposal> proposalCaptor = ArgumentCaptor.forClass(RentalProposal.class);
        verify(proposalMapper).insert(proposalCaptor.capture());
        RentalProposal savedProposal = proposalCaptor.getValue();
        assertThat(savedProposal.getRentAmount()).isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(savedProposal.getDepositAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void createApplication_rentOwnsItem_rejected() {
        when(itemClient.getItemInfo(50L)).thenReturn(
                ApiResponse.success(new ItemInfoFeignResponse(50L, 10L, "电钻", 1, 0,
                        new BigDecimal("10.00"), new BigDecimal("100.00"), 1, 1, 0)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createApplication(10L, validCreateRequest()));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_OWNER_CANNOT_RENT_OWN_ITEM);
    }

    @Test
    void createApplication_itemNotListed_rejected() {
        when(itemClient.getItemInfo(50L)).thenReturn(
                ApiResponse.success(new ItemInfoFeignResponse(50L, 20L, "电钻", 1, 0,
                        new BigDecimal("10.00"), new BigDecimal("100.00"), 1, 2, 0)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createApplication(10L, validCreateRequest()));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_ITEM_UNAVAILABLE);
    }

    @Test
    void createApplication_itemAuditRectify_rejected() {
        when(itemClient.getItemInfo(50L)).thenReturn(
                ApiResponse.success(new ItemInfoFeignResponse(50L, 20L, "电钻", 1, 0,
                        new BigDecimal("10.00"), new BigDecimal("100.00"), 1, 1, 2)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createApplication(10L, validCreateRequest()));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_ITEM_UNAVAILABLE);
    }

    @Test
    void createApplication_renterBanned_rejected() {
        when(itemClient.getItemInfo(50L)).thenReturn(
                ApiResponse.success(new ItemInfoFeignResponse(50L, 20L, "电钻", 1, 0,
                        new BigDecimal("10.00"), new BigDecimal("100.00"), 1, 1, 0)));
        when(authClient.getUserStatus(10L)).thenReturn(
                ApiResponse.success(new UserStatusFeignResponse(10L, 1, 100, 0)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createApplication(10L, validCreateRequest()));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_ITEM_UNAVAILABLE);
    }

    @Test
    void createApplication_walletNotUsable_rejected() {
        when(itemClient.getItemInfo(50L)).thenReturn(
                ApiResponse.success(new ItemInfoFeignResponse(50L, 20L, "电钻", 1, 0,
                        new BigDecimal("10.00"), new BigDecimal("100.00"), 1, 1, 0)));
        when(authClient.getUserStatus(10L)).thenReturn(
                ApiResponse.success(new UserStatusFeignResponse(10L, 0, 100, 0)));
        when(walletClient.isUsable(10L)).thenReturn(
                ApiResponse.success(new WalletUsableResponse(10L, false, BigDecimal.ZERO)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createApplication(10L, validCreateRequest()));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_ITEM_UNAVAILABLE);
    }

    @Test
    void createApplication_invalidTimeRange_rejected() {
        when(itemClient.getItemInfo(50L)).thenReturn(
                ApiResponse.success(new ItemInfoFeignResponse(50L, 20L, "电钻", 1, 0,
                        new BigDecimal("10.00"), new BigDecimal("100.00"), 1, 1, 0)));
        when(authClient.getUserStatus(10L)).thenReturn(
                ApiResponse.success(new UserStatusFeignResponse(10L, 0, 100, 0)));
        when(walletClient.isUsable(10L)).thenReturn(
                ApiResponse.success(new WalletUsableResponse(10L, true, new BigDecimal("88.50"))));

        RentalApplicationCreateRequest req = validCreateRequest();
        req.setRentEndTime(req.getRentStartTime());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createApplication(10L, req));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_TIME_INVALID);
    }

    @Test
    void createApplication_duplicateSubmitRejectedBeforeInsert() {
        when(itemClient.getItemInfo(50L)).thenReturn(
                ApiResponse.success(new ItemInfoFeignResponse(50L, 20L, "电钻", 1, 0,
                        new BigDecimal("10.00"), new BigDecimal("100.00"), 1, 1, 0)));
        when(authClient.getUserStatus(10L)).thenReturn(
                ApiResponse.success(new UserStatusFeignResponse(10L, 0, 100, 0)));
        when(walletClient.isUsable(10L)).thenReturn(
                ApiResponse.success(new WalletUsableResponse(10L, true, new BigDecimal("88.50"))));
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createApplication(10L, validCreateRequest()));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.REQUEST_TOO_FREQUENT);
        verify(applicationMapper, never()).insert(any(RentalApplication.class));
        verify(proposalMapper, never()).insert(any(RentalProposal.class));
    }

    @Test
    void createApplication_sendsApplicationCardToOwner() {
        when(itemClient.getItemInfo(50L)).thenReturn(
                ApiResponse.success(new ItemInfoFeignResponse(50L, 20L, "电钻", 1, 0,
                        new BigDecimal("10.00"), new BigDecimal("100.00"), 1, 1, 0)));
        when(authClient.getUserStatus(10L)).thenReturn(
                ApiResponse.success(new UserStatusFeignResponse(10L, 0, 100, 0)));
        when(walletClient.isUsable(10L)).thenReturn(
                ApiResponse.success(new WalletUsableResponse(10L, true, new BigDecimal("88.50"))));
        when(applicationMapper.insert(any(RentalApplication.class))).thenAnswer(inv -> {
            RentalApplication app = inv.getArgument(0);
            app.setId(100L);
            return 1;
        });
        when(proposalMapper.insert(any(RentalProposal.class))).thenAnswer(inv -> {
            RentalProposal p = inv.getArgument(0);
            p.setId(200L);
            return 1;
        });
        lenient().when(applicationMapper.updateById(any(RentalApplication.class))).thenReturn(1);

        service.createApplication(10L, validCreateRequest());

        ArgumentCaptor<CardMessageRequest> cardCaptor = ArgumentCaptor.forClass(CardMessageRequest.class);
        verify(messageRentalClient, times(1)).createCardMessage(cardCaptor.capture());
        CardMessageRequest card = cardCaptor.getValue();
        assertThat(card.getSenderId()).isEqualTo(10L);
        assertThat(card.getReceiverId()).isEqualTo(20L);
        assertThat(card.getItemId()).isEqualTo(50L);
        assertThat(card.getContent()).contains("电钻");
        assertThat(card.getCardType()).isEqualTo(1);
        assertThat(card.getRelatedApplicationId()).isEqualTo(100L);
        assertThat(card.getRelatedOrderId()).isNull();
    }

    @Test
    void createApplication_stillSucceedsWhenMessageCardFails() {
        when(itemClient.getItemInfo(50L)).thenReturn(
                ApiResponse.success(new ItemInfoFeignResponse(50L, 20L, "电钻", 1, 0,
                        new BigDecimal("10.00"), new BigDecimal("100.00"), 1, 1, 0)));
        when(authClient.getUserStatus(10L)).thenReturn(
                ApiResponse.success(new UserStatusFeignResponse(10L, 0, 100, 0)));
        when(walletClient.isUsable(10L)).thenReturn(
                ApiResponse.success(new WalletUsableResponse(10L, true, new BigDecimal("88.50"))));
        when(applicationMapper.insert(any(RentalApplication.class))).thenAnswer(inv -> {
            RentalApplication app = inv.getArgument(0);
            app.setId(100L);
            return 1;
        });
        when(proposalMapper.insert(any(RentalProposal.class))).thenAnswer(inv -> {
            RentalProposal p = inv.getArgument(0);
            p.setId(200L);
            return 1;
        });
        lenient().when(applicationMapper.updateById(any(RentalApplication.class))).thenReturn(1);
        doThrow(new RuntimeException("message-service down"))
                .when(messageRentalClient).createCardMessage(any(CardMessageRequest.class));

        RentalApplicationDetailResponse resp = service.createApplication(10L, validCreateRequest());

        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(100L);
        verify(applicationMapper, times(1)).insert(any(RentalApplication.class));
        verify(messageRentalClient, times(1)).createCardMessage(any(CardMessageRequest.class));
    }

    @Test
    void updateProposal_participantIncrementsVersion() {
        RentalApplication app = negotiatingApplication(1L, 200L);
        RentalProposal currentProposal = currentProposal(200L, 1L, 1, 10L);
        when(applicationMapper.selectById(1L)).thenReturn(app);
        when(proposalMapper.selectById(200L)).thenReturn(currentProposal);
        when(proposalMapper.selectList(any())).thenReturn(List.of(currentProposal));
        when(proposalMapper.insert(any(RentalProposal.class))).thenAnswer(inv -> {
            RentalProposal p = inv.getArgument(0);
            p.setId(201L);
            return 1;
        });
        lenient().when(applicationMapper.updateById(any(RentalApplication.class))).thenReturn(1);

        RentalProposalUpdateRequest req = new RentalProposalUpdateRequest();
        req.setQuantity(2);
        req.setRentAmount(new BigDecimal("120.00"));

        RentalApplicationDetailResponse resp = service.updateProposal(1L, 10L, req);

        ArgumentCaptor<RentalProposal> proposalCaptor = ArgumentCaptor.forClass(RentalProposal.class);
        verify(proposalMapper).insert(proposalCaptor.capture());
        RentalProposal saved = proposalCaptor.getValue();
        assertThat(saved.getApplicationId()).isEqualTo(1L);
        assertThat(saved.getVersionNo()).isEqualTo(2);
        assertThat(saved.getOperatorId()).isEqualTo(10L);
        assertThat(saved.getQuantity()).isEqualTo(2);
        assertThat(saved.getRentAmount()).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(saved.getChangedFields()).contains("quantity").contains("rentAmount");

        ArgumentCaptor<RentalApplication> appCaptor = ArgumentCaptor.forClass(RentalApplication.class);
        verify(applicationMapper).updateById(appCaptor.capture());
        assertThat(appCaptor.getValue().getCurrentProposalId()).isEqualTo(201L);
        assertThat(appCaptor.getValue().getRenterConfirmed()).isEqualTo(1);
        assertThat(appCaptor.getValue().getOwnerConfirmed()).isEqualTo(0);

        assertThat(resp.getCurrentProposal().getVersionNo()).isEqualTo(2);
        assertThat(resp.getCurrentProposal().getId()).isEqualTo(201L);
    }

    @Test
    void updateProposal_nonParticipant_rejected() {
        RentalApplication app = negotiatingApplication(1L, 200L);
        when(applicationMapper.selectById(1L)).thenReturn(app);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateProposal(1L, 99L, new RentalProposalUpdateRequest()));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_APPLICATION_PERMISSION_DENIED);
        verify(proposalMapper, never()).insert(any(RentalProposal.class));
    }

    @Test
    void updateProposal_cancelledApplication_rejected() {
        RentalApplication app = negotiatingApplication(1L, 200L);
        app.setStatus(ApplicationStatusEnum.CANCELLED.code());
        when(applicationMapper.selectById(1L)).thenReturn(app);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateProposal(1L, 10L, new RentalProposalUpdateRequest()));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_APPLICATION_STATUS_INVALID);
    }

    @Test
    void updateProposal_convertedApplication_rejected() {
        RentalApplication app = negotiatingApplication(1L, 200L);
        app.setStatus(ApplicationStatusEnum.CONVERTED.code());
        when(applicationMapper.selectById(1L)).thenReturn(app);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateProposal(1L, 10L, new RentalProposalUpdateRequest()));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_APPLICATION_STATUS_INVALID);
    }

    @Test
    void confirm_nonParticipant_rejected() {
        RentalApplication app = negotiatingApplication(1L, 200L);
        when(applicationMapper.selectById(1L)).thenReturn(app);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.confirm(1L, 99L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_APPLICATION_PERMISSION_DENIED);
        verify(orderService, never()).createOrder(any(RentalApplication.class), any(RentalProposal.class));
    }

    @Test
    void confirm_bothSidesConfirmed_createsExactlyOneOrder() {
        RentalApplication app = negotiatingApplication(1L, 200L);
        app.setOwnerConfirmed(0);
        app.setRenterConfirmed(1);
        RentalProposal proposal = currentProposal(200L, 1L, 1, 10L);
        when(applicationMapper.selectById(1L)).thenReturn(app);
        when(proposalMapper.selectById(200L)).thenReturn(proposal);
        doAnswer(inv -> {
            RentalApplication a = inv.getArgument(0);
            a.setStatus(ApplicationStatusEnum.CONVERTED.code());
            return null;
        }).when(orderService).createOrder(any(RentalApplication.class), any(RentalProposal.class));
        lenient().when(applicationMapper.updateById(any(RentalApplication.class))).thenReturn(1);

        RentalApplicationDetailResponse resp = service.confirm(1L, 20L);

        verify(orderService, times(1)).createOrder(any(RentalApplication.class), any(RentalProposal.class));
        assertThat(resp.getStatus()).isEqualTo(ApplicationStatusEnum.CONVERTED.code());
        assertThat(resp.getOwnerConfirmed()).isEqualTo(1);
        assertThat(resp.getRenterConfirmed()).isEqualTo(1);
    }

    @Test
    void confirm_onlyOneSideConfirmed_doesNotCreateOrder() {
        RentalApplication app = negotiatingApplication(1L, 200L);
        app.setOwnerConfirmed(0);
        app.setRenterConfirmed(0);
        when(applicationMapper.selectById(1L)).thenReturn(app);
        lenient().when(proposalMapper.selectById(200L)).thenReturn(currentProposal(200L, 1L, 1, 10L));
        lenient().when(applicationMapper.updateById(any(RentalApplication.class))).thenReturn(1);

        RentalApplicationDetailResponse resp = service.confirm(1L, 20L);

        verify(orderService, never()).createOrder(any(RentalApplication.class), any(RentalProposal.class));
        assertThat(resp.getStatus()).isEqualTo(ApplicationStatusEnum.NEGOTIATING.code());
        assertThat(resp.getOwnerConfirmed()).isEqualTo(1);
    }

    @Test
    void listApplications_returnsApplicationsForUser() {
        RentalApplication app = negotiatingApplication(1L, 200L);
        when(applicationMapper.selectList(any())).thenReturn(List.of(app));

        List<RentalApplicationResponse> list = service.listApplications(10L);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo(1L);
        assertThat(list.get(0).getStatus()).isEqualTo(ApplicationStatusEnum.NEGOTIATING.code());
        assertThat(list.get(0).getCurrentProposal()).isNull();
    }

    @Test
    void getApplication_participantReturnsDetailWithProposal() {
        RentalApplication app = negotiatingApplication(1L, 200L);
        RentalProposal proposal = currentProposal(200L, 1L, 1, 10L);
        when(applicationMapper.selectById(1L)).thenReturn(app);
        when(proposalMapper.selectById(200L)).thenReturn(proposal);

        RentalApplicationDetailResponse resp = service.getApplication(1L, 10L);

        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getCurrentProposal()).isNotNull();
        assertThat(resp.getCurrentProposal().getId()).isEqualTo(200L);
    }

    @Test
    void getApplication_noCurrentProposal_returnsDetailWithoutProposal() {
        RentalApplication app = negotiatingApplication(1L, null);
        when(applicationMapper.selectById(1L)).thenReturn(app);

        RentalApplicationDetailResponse resp = service.getApplication(1L, 10L);

        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getCurrentProposal()).isNull();
        verify(proposalMapper, never()).selectById(any());
    }

    @Test
    void getApplication_notFound_rejected() {
        when(applicationMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getApplication(1L, 10L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_APPLICATION_NOT_FOUND);
    }

    @Test
    void getApplication_nonParticipant_rejected() {
        RentalApplication app = negotiatingApplication(1L, 200L);
        when(applicationMapper.selectById(1L)).thenReturn(app);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getApplication(1L, 99L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_APPLICATION_PERMISSION_DENIED);
    }

    @Test
    void cancel_participant_setsCancelled() {
        RentalApplication app = negotiatingApplication(1L, 200L);
        when(applicationMapper.selectById(1L)).thenReturn(app);
        when(applicationMapper.updateById(any(RentalApplication.class))).thenReturn(1);

        service.cancel(1L, 10L);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatusEnum.CANCELLED.code());
        verify(applicationMapper).updateById(app);
    }

    @Test
    void cancel_nonParticipant_rejected() {
        RentalApplication app = negotiatingApplication(1L, 200L);
        when(applicationMapper.selectById(1L)).thenReturn(app);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.cancel(1L, 99L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_APPLICATION_PERMISSION_DENIED);
        verify(applicationMapper, never()).updateById(any(RentalApplication.class));
    }

    @Test
    void cancel_invalidStatus_rejected() {
        RentalApplication app = negotiatingApplication(1L, 200L);
        app.setStatus(ApplicationStatusEnum.CONVERTED.code());
        when(applicationMapper.selectById(1L)).thenReturn(app);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.cancel(1L, 10L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_APPLICATION_STATUS_INVALID);
        verify(applicationMapper, never()).updateById(any(RentalApplication.class));
    }

    @Test
    void updateProposal_invalidTimeRange_rejected() {
        RentalApplication app = negotiatingApplication(1L, 200L);
        RentalProposal currentProposal = currentProposal(200L, 1L, 1, 10L);
        when(applicationMapper.selectById(1L)).thenReturn(app);
        when(proposalMapper.selectById(200L)).thenReturn(currentProposal);
        when(proposalMapper.selectList(any())).thenReturn(List.of(currentProposal));

        RentalProposalUpdateRequest req = new RentalProposalUpdateRequest();
        req.setRentStartTime(LocalDateTime.of(2026, 7, 5, 10, 0));
        req.setRentEndTime(LocalDateTime.of(2026, 7, 3, 10, 0));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateProposal(1L, 10L, req));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_TIME_INVALID);
        verify(proposalMapper, never()).insert(any(RentalProposal.class));
    }

    private RentalApplication negotiatingApplication(Long id, Long currentProposalId) {
        RentalApplication app = new RentalApplication();
        app.setId(id);
        app.setItemId(50L);
        app.setOwnerId(20L);
        app.setRenterId(10L);
        app.setStatus(ApplicationStatusEnum.NEGOTIATING.code());
        app.setCurrentProposalId(currentProposalId);
        app.setOwnerConfirmed(0);
        app.setRenterConfirmed(1);
        return app;
    }

    private RentalProposal currentProposal(Long id, Long applicationId, Integer versionNo, Long operatorId) {
        RentalProposal p = new RentalProposal();
        p.setId(id);
        p.setApplicationId(applicationId);
        p.setVersionNo(versionNo);
        p.setOperatorId(operatorId);
        p.setQuantity(1);
        p.setDeliveryType(1);
        p.setRentStartTime(LocalDateTime.of(2026, 7, 1, 10, 0));
        p.setRentEndTime(LocalDateTime.of(2026, 7, 3, 10, 0));
        p.setRentAmount(new BigDecimal("60.00"));
        p.setDepositAmount(new BigDecimal("100.00"));
        p.setReceiverName("张三");
        p.setReceiverPhone("13800000000");
        p.setReceiverAddress("北京市朝阳区");
        return p;
    }

    private RentalApplicationCreateRequest validCreateRequest() {
        RentalApplicationCreateRequest req = new RentalApplicationCreateRequest();
        req.setItemId(50L);
        req.setQuantity(1);
        req.setRentStartTime(LocalDateTime.of(2026, 7, 1, 10, 0));
        req.setRentEndTime(LocalDateTime.of(2026, 7, 3, 10, 0));
        req.setDeliveryType(1);
        req.setReceiverName("张三");
        req.setReceiverPhone("13800000000");
        req.setReceiverAddress("北京市朝阳区");
        req.setRentAmount(new BigDecimal("60.00"));
        req.setDepositAmount(new BigDecimal("100.00"));
        return req;
    }
}
