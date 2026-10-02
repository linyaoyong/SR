package com.share.rental.rental.service;

import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.response.ApiResponse;
import com.share.rental.rental.client.ItemRentalClient;
import com.share.rental.rental.client.MessageRentalClient;
import com.share.rental.rental.client.WalletRentalClient;
import com.share.rental.rental.client.AuthRentalClient;
import com.share.rental.rental.dto.CreditScoreAdjustFeignRequest;
import com.share.rental.rental.dto.CancelOrderRequest;
import com.share.rental.rental.dto.CancelPaymentFeignRequest;
import com.share.rental.rental.dto.CompleteSettlementFeignRequest;
import com.share.rental.rental.dto.ItemInfoFeignResponse;
import com.share.rental.rental.dto.RentalOrderResponse;
import com.share.rental.rental.dto.ReturnOrderRequest;
import com.share.rental.rental.dto.SettlementResponse;
import com.share.rental.rental.dto.ShipOrderRequest;
import com.share.rental.rental.entity.OrderStatusHistory;
import com.share.rental.rental.entity.RentalApplication;
import com.share.rental.rental.entity.RentalOrder;
import com.share.rental.rental.entity.RentalProposal;
import com.share.rental.rental.enums.ApplicationStatusEnum;
import com.share.rental.rental.enums.DeliveryTypeEnum;
import com.share.rental.rental.enums.OrderStatusEnum;
import com.share.rental.rental.mapper.OrderStatusHistoryMapper;
import com.share.rental.rental.mapper.RentalApplicationMapper;
import com.share.rental.rental.mapper.RentalOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalOrderServiceTest {

    @Mock
    private RentalOrderMapper orderMapper;
    @Mock
    private OrderStatusHistoryMapper historyMapper;
    @Mock
    private RentalApplicationMapper applicationMapper;
    @Mock
    private ItemRentalClient itemClient;
    @Mock
    private RentalTimeLockService timeLockService;
    @Mock
    private OrderNumberGenerator orderNumberGenerator;
    @Mock
    private WalletRentalClient walletClient;
    @Mock
    private MessageRentalClient messageClient;
    @Mock
    private AuthRentalClient authClient;
    @Mock
    private PaymentTimeoutProducer paymentTimeoutProducer;

    @InjectMocks
    private RentalOrderService service;

    @BeforeEach
    void setUpRemoteDefaults() {
        lenient().when(itemClient.reserveRentedCount(any(), any())).thenReturn(ApiResponse.success());
        lenient().when(itemClient.releaseRentedCount(any(), any())).thenReturn(ApiResponse.success());
    }

    @Test
    void createOrder_createsOrderSnapshotTimeLockAndHistory() {
        RentalApplication application = application();
        RentalProposal proposal = proposal();
        ItemInfoFeignResponse item = new ItemInfoFeignResponse(50L, 20L, "电钻", 2, 0,
                new BigDecimal("10.00"), new BigDecimal("100.00"), 1, 1, 0);
        when(itemClient.getItemInfo(50L)).thenReturn(ApiResponse.success(item));
        when(itemClient.createSnapshot(50L)).thenReturn(ApiResponse.success(500L));
        when(orderNumberGenerator.next()).thenReturn("SR202606271200001234");
        when(orderMapper.insert(any(RentalOrder.class))).thenAnswer(inv -> {
            RentalOrder o = inv.getArgument(0);
            o.setId(1L);
            return 1;
        });
        lenient().when(historyMapper.insert(any(OrderStatusHistory.class))).thenReturn(1);
        lenient().when(applicationMapper.updateById(any(RentalApplication.class))).thenReturn(1);

        service.createOrder(application, proposal);

        ArgumentCaptor<RentalOrder> orderCaptor = ArgumentCaptor.forClass(RentalOrder.class);
        verify(orderMapper).insert(orderCaptor.capture());
        RentalOrder savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getOrderNo()).isEqualTo("SR202606271200001234");
        assertThat(savedOrder.getApplicationId()).isEqualTo(100L);
        assertThat(savedOrder.getProposalId()).isEqualTo(200L);
        assertThat(savedOrder.getItemId()).isEqualTo(50L);
        assertThat(savedOrder.getItemSnapshotId()).isEqualTo(500L);
        assertThat(savedOrder.getOwnerId()).isEqualTo(20L);
        assertThat(savedOrder.getRenterId()).isEqualTo(10L);
        assertThat(savedOrder.getQuantity()).isEqualTo(1);
        assertThat(savedOrder.getDeliveryType()).isEqualTo(1);
        assertThat(savedOrder.getDailyPrice()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(savedOrder.getRentAmount()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(savedOrder.getDepositAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(savedOrder.getPaidRentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(savedOrder.getFrozenDepositAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatusEnum.PENDING_PAYMENT.code());

        verify(timeLockService).createLockIfAvailable(eq(1L), eq(50L), eq(1),
                eq(LocalDateTime.of(2026, 7, 1, 10, 0)), eq(LocalDateTime.of(2026, 7, 3, 10, 0)), eq(2));
        verify(itemClient).reserveRentedCount(50L, 1);
        verify(timeLockService, never()).checkOverlap(any(), any(), any(), any(), any());
        verify(timeLockService, never()).createLock(any(), any(), any(), any(), any());
        verify(paymentTimeoutProducer).publish(1L);

        ArgumentCaptor<OrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(historyMapper).insert(historyCaptor.capture());
        OrderStatusHistory savedHistory = historyCaptor.getValue();
        assertThat(savedHistory.getOrderId()).isEqualTo(1L);
        assertThat(savedHistory.getOldStatus()).isNull();
        assertThat(savedHistory.getNewStatus()).isEqualTo(OrderStatusEnum.PENDING_PAYMENT.code());

        ArgumentCaptor<RentalApplication> appCaptor = ArgumentCaptor.forClass(RentalApplication.class);
        verify(applicationMapper).updateById(appCaptor.capture());
        assertThat(appCaptor.getValue().getStatus()).isEqualTo(ApplicationStatusEnum.CONVERTED.code());
    }

    @Test
    void createOrder_itemQuantityNull_rejected() {
        RentalApplication application = application();
        RentalProposal proposal = proposal();
        ItemInfoFeignResponse item = new ItemInfoFeignResponse(50L, 20L, "电钻", null, 0,
                new BigDecimal("10.00"), new BigDecimal("100.00"), 1, 1, 0);
        when(itemClient.getItemInfo(50L)).thenReturn(ApiResponse.success(item));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createOrder(application, proposal));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_ITEM_UNAVAILABLE);
        verify(timeLockService, never()).checkOverlap(any(), any(), any(), any(), any());
        verify(orderMapper, never()).insert(any(RentalOrder.class));
    }

    @Test
    void listOrders_returnsOrdersForUser() {
        when(orderMapper.selectList(any())).thenReturn(List.of(order(1L, OrderStatusEnum.PENDING_PAYMENT.code())));

        List<RentalOrderResponse> list = service.listOrders(10L);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void getOrder_participantReturnsDetail() {
        RentalOrder o = order(1L, OrderStatusEnum.PENDING_PAYMENT.code());
        when(orderMapper.selectById(1L)).thenReturn(o);

        RentalOrderResponse resp = service.getOrder(1L, 10L);

        assertThat(resp.getId()).isEqualTo(1L);
    }

    @Test
    void getOrder_notFound_rejected() {
        when(orderMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getOrder(1L, 10L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_ORDER_NOT_FOUND);
    }

    @Test
    void getOrder_nonParticipant_rejected() {
        RentalOrder o = order(1L, OrderStatusEnum.PENDING_PAYMENT.code());
        when(orderMapper.selectById(1L)).thenReturn(o);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getOrder(1L, 99L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_APPLICATION_PERMISSION_DENIED);
    }

    @Test
    void cancel_pendingPayment_releasesLockAndWritesHistory() {
        RentalOrder o = order(1L, OrderStatusEnum.PENDING_PAYMENT.code());
        when(orderMapper.selectById(1L)).thenReturn(o);
        lenient().when(orderMapper.updateById(any(RentalOrder.class))).thenReturn(1);
        lenient().when(historyMapper.insert(any(OrderStatusHistory.class))).thenReturn(1);

        CancelOrderRequest req = new CancelOrderRequest();
        req.setCancelReason("不想要了");
        service.cancel(1L, 10L, req);

        assertThat(o.getStatus()).isEqualTo(OrderStatusEnum.CANCELLED.code());
        assertThat(o.getCancelReason()).isEqualTo("不想要了");
        verify(timeLockService).releaseLocks(1L);
        verify(itemClient).releaseRentedCount(50L, 1);
        ArgumentCaptor<OrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(historyMapper).insert(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getNewStatus()).isEqualTo(OrderStatusEnum.CANCELLED.code());
    }

    @Test
    void cancel_invalidStatus_rejected() {
        RentalOrder o = order(1L, OrderStatusEnum.RENTING.code());
        when(orderMapper.selectById(1L)).thenReturn(o);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.cancel(1L, 10L, new CancelOrderRequest()));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_ORDER_STATUS_INVALID);
        verify(timeLockService, never()).releaseLocks(any());
    }

    @Test
    void cancel_nonParticipant_rejected() {
        RentalOrder o = order(1L, OrderStatusEnum.PENDING_PAYMENT.code());
        when(orderMapper.selectById(1L)).thenReturn(o);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.cancel(1L, 99L, new CancelOrderRequest()));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_APPLICATION_PERMISSION_DENIED);
    }

    @Test
    void ship_paidPendingDeliveryExpress_success() {
        RentalOrder o = order(1L, OrderStatusEnum.PAID_PENDING_DELIVERY.code());
        o.setDeliveryType(DeliveryTypeEnum.EXPRESS.code());
        when(orderMapper.selectById(1L)).thenReturn(o);
        lenient().when(orderMapper.updateById(any(RentalOrder.class))).thenReturn(1);
        lenient().when(historyMapper.insert(any(OrderStatusHistory.class))).thenReturn(1);

        ShipOrderRequest req = new ShipOrderRequest();
        req.setShipCompany("顺丰");
        req.setShipTrackingNo("SF1234567890");
        service.ship(1L, 20L, req);

        assertThat(o.getStatus()).isEqualTo(OrderStatusEnum.SHIPPED.code());
        assertThat(o.getShipCompany()).isEqualTo("顺丰");
        assertThat(o.getShipTrackingNo()).isEqualTo("SF1234567890");
        verify(historyMapper).insert(any(OrderStatusHistory.class));
    }

    @Test
    void ship_nonOwner_rejected() {
        RentalOrder o = order(1L, OrderStatusEnum.PAID_PENDING_DELIVERY.code());
        o.setDeliveryType(DeliveryTypeEnum.EXPRESS.code());
        when(orderMapper.selectById(1L)).thenReturn(o);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.ship(1L, 10L, new ShipOrderRequest()));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_APPLICATION_PERMISSION_DENIED);
    }

    @Test
    void ship_meetupDelivery_rejected() {
        RentalOrder o = order(1L, OrderStatusEnum.PAID_PENDING_DELIVERY.code());
        o.setDeliveryType(DeliveryTypeEnum.MEETUP.code());
        when(orderMapper.selectById(1L)).thenReturn(o);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.ship(1L, 20L, new ShipOrderRequest()));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_ORDER_STATUS_INVALID);
    }

    @Test
    void ship_invalidStatus_rejected() {
        RentalOrder o = order(1L, OrderStatusEnum.PENDING_PAYMENT.code());
        o.setDeliveryType(DeliveryTypeEnum.EXPRESS.code());
        when(orderMapper.selectById(1L)).thenReturn(o);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.ship(1L, 20L, new ShipOrderRequest()));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_ORDER_STATUS_INVALID);
    }

    @Test
    void receive_expressShipped_toRenting() {
        RentalOrder o = order(1L, OrderStatusEnum.SHIPPED.code());
        o.setDeliveryType(DeliveryTypeEnum.EXPRESS.code());
        when(orderMapper.selectById(1L)).thenReturn(o);
        lenient().when(orderMapper.updateById(any(RentalOrder.class))).thenReturn(1);
        lenient().when(historyMapper.insert(any(OrderStatusHistory.class))).thenReturn(1);

        service.receive(1L, 10L);

        assertThat(o.getStatus()).isEqualTo(OrderStatusEnum.RENTING.code());
        assertThat(o.getReceivedTime()).isNotNull();
    }

    @Test
    void receive_meetupPaidPendingDelivery_toRenting() {
        RentalOrder o = order(1L, OrderStatusEnum.PAID_PENDING_DELIVERY.code());
        o.setDeliveryType(DeliveryTypeEnum.MEETUP.code());
        when(orderMapper.selectById(1L)).thenReturn(o);
        lenient().when(orderMapper.updateById(any(RentalOrder.class))).thenReturn(1);
        lenient().when(historyMapper.insert(any(OrderStatusHistory.class))).thenReturn(1);

        service.receive(1L, 10L);

        assertThat(o.getStatus()).isEqualTo(OrderStatusEnum.RENTING.code());
        assertThat(o.getReceivedTime()).isNotNull();
    }

    @Test
    void receive_nonRenter_rejected() {
        RentalOrder o = order(1L, OrderStatusEnum.SHIPPED.code());
        o.setDeliveryType(DeliveryTypeEnum.EXPRESS.code());
        when(orderMapper.selectById(1L)).thenReturn(o);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.receive(1L, 20L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_APPLICATION_PERMISSION_DENIED);
    }

    @Test
    void receive_expressInvalidStatus_rejected() {
        RentalOrder o = order(1L, OrderStatusEnum.PENDING_PAYMENT.code());
        o.setDeliveryType(DeliveryTypeEnum.EXPRESS.code());
        when(orderMapper.selectById(1L)).thenReturn(o);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.receive(1L, 10L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_ORDER_STATUS_INVALID);
    }

    @Test
    void returnOrder_renting_success() {
        RentalOrder o = order(1L, OrderStatusEnum.RENTING.code());
        o.setDeliveryType(DeliveryTypeEnum.EXPRESS.code());
        when(orderMapper.selectById(1L)).thenReturn(o);
        lenient().when(orderMapper.updateById(any(RentalOrder.class))).thenReturn(1);
        lenient().when(historyMapper.insert(any(OrderStatusHistory.class))).thenReturn(1);

        ReturnOrderRequest req = new ReturnOrderRequest();
        req.setReturnCompany("顺丰");
        req.setReturnTrackingNo("SF9876543210");
        service.returnOrder(1L, 10L, req);

        assertThat(o.getStatus()).isEqualTo(OrderStatusEnum.PENDING_RETURN_CONFIRM.code());
        assertThat(o.getReturnCompany()).isEqualTo("顺丰");
        assertThat(o.getReturnTrackingNo()).isEqualTo("SF9876543210");
    }

    @Test
    void returnOrder_emptyRequestBody_stillTransitions() {
        RentalOrder o = order(1L, OrderStatusEnum.RENTING.code());
        when(orderMapper.selectById(1L)).thenReturn(o);
        lenient().when(orderMapper.updateById(any(RentalOrder.class))).thenReturn(1);
        lenient().when(historyMapper.insert(any(OrderStatusHistory.class))).thenReturn(1);

        service.returnOrder(1L, 10L, null);

        assertThat(o.getStatus()).isEqualTo(OrderStatusEnum.PENDING_RETURN_CONFIRM.code());
        verify(historyMapper).insert(any(OrderStatusHistory.class));
    }

    @Test
    void returnOrder_nonRenter_rejected() {
        RentalOrder o = order(1L, OrderStatusEnum.RENTING.code());
        when(orderMapper.selectById(1L)).thenReturn(o);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.returnOrder(1L, 20L, new ReturnOrderRequest()));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_APPLICATION_PERMISSION_DENIED);
    }

    @Test
    void returnOrder_invalidStatus_rejected() {
        RentalOrder o = order(1L, OrderStatusEnum.SHIPPED.code());
        when(orderMapper.selectById(1L)).thenReturn(o);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.returnOrder(1L, 10L, new ReturnOrderRequest()));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_ORDER_STATUS_INVALID);
    }

    @Test
    void complete_pendingReturnConfirm_releasesLockAndWritesHistory() {
        RentalOrder o = order(1L, OrderStatusEnum.PENDING_RETURN_CONFIRM.code());
        when(orderMapper.selectById(1L)).thenReturn(o);
        lenient().when(orderMapper.updateById(any(RentalOrder.class))).thenReturn(1);
        lenient().when(historyMapper.insert(any(OrderStatusHistory.class))).thenReturn(1);

        service.complete(1L, 20L);

        assertThat(o.getStatus()).isEqualTo(OrderStatusEnum.COMPLETED.code());
        assertThat(o.getCompletedTime()).isNotNull();
        verify(timeLockService).releaseLocks(1L);
        verify(itemClient).releaseRentedCount(50L, 1);
        verify(historyMapper).insert(any(OrderStatusHistory.class));
        verify(messageClient, times(2)).sendSystemNotification(any());
    }

    @Test
    void complete_nonOwner_rejected() {
        RentalOrder o = order(1L, OrderStatusEnum.PENDING_RETURN_CONFIRM.code());
        when(orderMapper.selectById(1L)).thenReturn(o);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.complete(1L, 10L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_APPLICATION_PERMISSION_DENIED);
    }

    @Test
    void complete_invalidStatus_rejected() {
        RentalOrder o = order(1L, OrderStatusEnum.RENTING.code());
        when(orderMapper.selectById(1L)).thenReturn(o);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.complete(1L, 20L));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_ORDER_STATUS_INVALID);
    }

    @Test
    void cancel_callsWalletCancelPayment() {
        RentalOrder o = order(1L, OrderStatusEnum.PENDING_PAYMENT.code());
        o.setPaidRentAmount(new BigDecimal("100.00"));
        o.setFrozenDepositAmount(new BigDecimal("50.00"));
        when(orderMapper.selectById(1L)).thenReturn(o);
        lenient().when(orderMapper.updateById(any(RentalOrder.class))).thenReturn(1);
        lenient().when(historyMapper.insert(any(OrderStatusHistory.class))).thenReturn(1);
        when(walletClient.cancelOrderPayment(eq(1L), any(CancelPaymentFeignRequest.class)))
                .thenReturn(ApiResponse.success());

        service.cancel(1L, 10L, new CancelOrderRequest());

        verify(walletClient).cancelOrderPayment(eq(1L), any(CancelPaymentFeignRequest.class));
    }

    @Test
    void cancel_zeroPayment_doesNotCallWallet() {
        RentalOrder o = order(1L, OrderStatusEnum.PENDING_PAYMENT.code());
        o.setPaidRentAmount(BigDecimal.ZERO);
        o.setFrozenDepositAmount(BigDecimal.ZERO);
        when(orderMapper.selectById(1L)).thenReturn(o);
        lenient().when(orderMapper.updateById(any(RentalOrder.class))).thenReturn(1);
        lenient().when(historyMapper.insert(any(OrderStatusHistory.class))).thenReturn(1);

        service.cancel(1L, 10L, new CancelOrderRequest());

        verify(walletClient, never()).cancelOrderPayment(eq(1L), any(CancelPaymentFeignRequest.class));
    }

    @Test
    void cancelPendingPaymentBySystem_onlyCancelsPendingPaymentOrder() {
        RentalOrder o = order(1L, OrderStatusEnum.PENDING_PAYMENT.code());
        when(orderMapper.selectById(1L)).thenReturn(o);
        lenient().when(orderMapper.updateById(any(RentalOrder.class))).thenReturn(1);
        lenient().when(historyMapper.insert(any(OrderStatusHistory.class))).thenReturn(1);

        boolean cancelled = service.cancelPendingPaymentBySystem(1L, "支付超时自动取消");

        assertThat(cancelled).isTrue();
        assertThat(o.getStatus()).isEqualTo(OrderStatusEnum.CANCELLED.code());
        assertThat(o.getCancelReason()).isEqualTo("支付超时自动取消");
        verify(timeLockService).releaseLocks(1L);
        verify(itemClient).releaseRentedCount(50L, 1);
    }

    @Test
    void cancelPendingPaymentBySystem_ignoresAlreadyPaidOrder() {
        RentalOrder o = order(1L, OrderStatusEnum.PAID_PENDING_DELIVERY.code());
        when(orderMapper.selectById(1L)).thenReturn(o);

        boolean cancelled = service.cancelPendingPaymentBySystem(1L, "支付超时自动取消");

        assertThat(cancelled).isFalse();
        verify(orderMapper, never()).updateById(any(RentalOrder.class));
        verify(timeLockService, never()).releaseLocks(any());
    }

    @Test
    void complete_callsWalletSettlement() {
        RentalOrder o = order(1L, OrderStatusEnum.PENDING_RETURN_CONFIRM.code());
        when(orderMapper.selectById(1L)).thenReturn(o);
        lenient().when(orderMapper.updateById(any(RentalOrder.class))).thenReturn(1);
        lenient().when(historyMapper.insert(any(OrderStatusHistory.class))).thenReturn(1);
        when(walletClient.completeSettlement(eq(1L), any(CompleteSettlementFeignRequest.class)))
                .thenReturn(ApiResponse.success(new SettlementResponse(
                        1L, true, true, new BigDecimal("20.00"), BigDecimal.ZERO)));

        service.complete(1L, 20L);

        verify(walletClient).completeSettlement(eq(1L), any(CompleteSettlementFeignRequest.class));
        assertThat(o.getOverdueFeeAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(o.getOverdueSettled()).isEqualTo(1);
        verify(messageClient, times(2)).sendSystemNotification(any());
    }

    @Test
    void complete_adjustsCreditForBothPartiesAndPenalizesOverdueRenter() {
        RentalOrder o = order(1L, OrderStatusEnum.PENDING_RETURN_CONFIRM.code());
        when(orderMapper.selectById(1L)).thenReturn(o);
        lenient().when(orderMapper.updateById(any(RentalOrder.class))).thenReturn(1);
        lenient().when(historyMapper.insert(any(OrderStatusHistory.class))).thenReturn(1);
        when(walletClient.completeSettlement(eq(1L), any(CompleteSettlementFeignRequest.class)))
                .thenReturn(ApiResponse.success(new SettlementResponse(
                        1L, true, true, new BigDecimal("20.00"), BigDecimal.ZERO)));

        service.complete(1L, 20L);

        verify(authClient).adjustCreditScore(eq(10L), argThat((CreditScoreAdjustFeignRequest request) ->
                Integer.valueOf(1).equals(request.getChangeValue())
                        && "ORDER_COMPLETED".equals(request.getReasonType())));
        verify(authClient).adjustCreditScore(eq(20L), argThat((CreditScoreAdjustFeignRequest request) ->
                Integer.valueOf(1).equals(request.getChangeValue())
                        && "ORDER_COMPLETED".equals(request.getReasonType())));
        verify(authClient).adjustCreditScore(eq(10L), argThat((CreditScoreAdjustFeignRequest request) ->
                Integer.valueOf(-5).equals(request.getChangeValue())
                        && "OVERDUE_RETURN".equals(request.getReasonType())));
    }

    private RentalOrder order(Long id, Integer status) {
        RentalOrder o = new RentalOrder();
        o.setId(id);
        o.setOrderNo("SR202606271200001234");
        o.setApplicationId(100L);
        o.setProposalId(200L);
        o.setItemId(50L);
        o.setItemSnapshotId(500L);
        o.setOwnerId(20L);
        o.setRenterId(10L);
        o.setQuantity(1);
        o.setDeliveryType(DeliveryTypeEnum.EXPRESS.code());
        o.setRentStartTime(LocalDateTime.of(2026, 7, 1, 10, 0));
        o.setRentEndTime(LocalDateTime.of(2026, 7, 3, 10, 0));
        o.setDailyPrice(new BigDecimal("10.00"));
        o.setRentAmount(new BigDecimal("60.00"));
        o.setDepositAmount(new BigDecimal("100.00"));
        o.setStatus(status);
        return o;
    }

    private RentalApplication application() {
        RentalApplication app = new RentalApplication();
        app.setId(100L);
        app.setItemId(50L);
        app.setOwnerId(20L);
        app.setRenterId(10L);
        app.setStatus(ApplicationStatusEnum.NEGOTIATING.code());
        app.setCurrentProposalId(200L);
        app.setPrepaidRentAmount(BigDecimal.ZERO);
        app.setPreFrozenDepositAmount(BigDecimal.ZERO);
        return app;
    }

    private RentalProposal proposal() {
        RentalProposal p = new RentalProposal();
        p.setId(200L);
        p.setApplicationId(100L);
        p.setVersionNo(1);
        p.setOperatorId(10L);
        p.setQuantity(1);
        p.setDeliveryType(1);
        p.setRentStartTime(LocalDateTime.of(2026, 7, 1, 10, 0));
        p.setRentEndTime(LocalDateTime.of(2026, 7, 3, 10, 0));
        p.setRentAmount(new BigDecimal("60.00"));
        p.setDepositAmount(new BigDecimal("100.00"));
        return p;
    }
}
