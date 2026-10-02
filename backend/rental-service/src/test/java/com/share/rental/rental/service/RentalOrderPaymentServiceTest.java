package com.share.rental.rental.service;

import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.response.ApiResponse;
import com.share.rental.rental.client.ItemRentalClient;
import com.share.rental.rental.client.WalletRentalClient;
import com.share.rental.rental.dto.FreezeDepositFeignRequest;
import com.share.rental.rental.dto.PrepayRentFeignRequest;
import com.share.rental.rental.dto.RentalOrderResponse;
import com.share.rental.rental.dto.WalletOperationResponse;
import com.share.rental.rental.entity.OrderStatusHistory;
import com.share.rental.rental.entity.RentalOrder;
import com.share.rental.rental.enums.DeliveryTypeEnum;
import com.share.rental.rental.enums.OrderStatusEnum;
import com.share.rental.rental.mapper.OrderStatusHistoryMapper;
import com.share.rental.rental.mapper.RentalApplicationMapper;
import com.share.rental.rental.mapper.RentalOrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalOrderPaymentServiceTest {

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
    private PaymentTimeoutProducer paymentTimeoutProducer;

    @InjectMocks
    private RentalOrderService service;

    @Test
    void payOrder_callsWalletAndUpdatesOrderPaidAmount() {
        RentalOrder o = order(1L, OrderStatusEnum.PENDING_PAYMENT.code(),
                new BigDecimal("60.00"), new BigDecimal("100.00"));
        when(orderMapper.selectById(1L)).thenReturn(o);
        when(walletClient.prepayRent(eq(1L), any(PrepayRentFeignRequest.class)))
                .thenReturn(ApiResponse.success(new WalletOperationResponse(
                        1L, new BigDecimal("100.00"), BigDecimal.ZERO, false)));
        lenient().when(orderMapper.updateById(any(RentalOrder.class))).thenReturn(1);
        lenient().when(historyMapper.insert(any(OrderStatusHistory.class))).thenReturn(1);

        RentalOrderResponse resp = service.payOrder(1L, 10L, new BigDecimal("100.00"));

        assertThat(resp.getPaidRentAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(o.getPaidRentAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(o.getStatus()).isEqualTo(OrderStatusEnum.PENDING_PAYMENT.code());
        verify(walletClient).prepayRent(eq(1L), any(PrepayRentFeignRequest.class));
    }

    @Test
    void payOrder_fullyPaid_advancesToPaidPendingDelivery() {
        RentalOrder o = order(1L, OrderStatusEnum.PENDING_PAYMENT.code(),
                new BigDecimal("100.00"), new BigDecimal("50.00"));
        when(orderMapper.selectById(1L)).thenReturn(o);
        when(walletClient.prepayRent(eq(1L), any(PrepayRentFeignRequest.class)))
                .thenReturn(ApiResponse.success(new WalletOperationResponse(
                        1L, new BigDecimal("100.00"), BigDecimal.ZERO, false)));
        when(walletClient.freezeDeposit(eq(1L), any(FreezeDepositFeignRequest.class)))
                .thenReturn(ApiResponse.success(new WalletOperationResponse(
                        1L, new BigDecimal("100.00"), new BigDecimal("50.00"), true)));
        lenient().when(orderMapper.updateById(any(RentalOrder.class))).thenReturn(1);
        lenient().when(historyMapper.insert(any(OrderStatusHistory.class))).thenReturn(1);

        service.payOrder(1L, 10L, new BigDecimal("100.00"));
        assertThat(o.getStatus()).isEqualTo(OrderStatusEnum.PENDING_PAYMENT.code());

        service.freezeDeposit(1L, 10L, new BigDecimal("50.00"));
        assertThat(o.getStatus()).isEqualTo(OrderStatusEnum.PAID_PENDING_DELIVERY.code());
    }

    @Test
    void payOrder_nonRenter_rejected() {
        RentalOrder o = order(1L, OrderStatusEnum.PENDING_PAYMENT.code(),
                new BigDecimal("60.00"), new BigDecimal("100.00"));
        when(orderMapper.selectById(1L)).thenReturn(o);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.payOrder(1L, 99L, new BigDecimal("60.00")));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_APPLICATION_PERMISSION_DENIED);
    }

    @Test
    void payOrder_wrongStatus_rejected() {
        RentalOrder o = order(1L, OrderStatusEnum.PAID_PENDING_DELIVERY.code(),
                new BigDecimal("60.00"), new BigDecimal("100.00"));
        when(orderMapper.selectById(1L)).thenReturn(o);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.payOrder(1L, 10L, new BigDecimal("60.00")));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_ORDER_STATUS_INVALID);
    }

    @Test
    void freezeDeposit_callsWalletAndUpdatesOrderFrozenAmount() {
        RentalOrder o = order(1L, OrderStatusEnum.PENDING_PAYMENT.code(),
                new BigDecimal("60.00"), new BigDecimal("100.00"));
        when(orderMapper.selectById(1L)).thenReturn(o);
        when(walletClient.freezeDeposit(eq(1L), any(FreezeDepositFeignRequest.class)))
                .thenReturn(ApiResponse.success(new WalletOperationResponse(
                        1L, BigDecimal.ZERO, new BigDecimal("50.00"), false)));
        lenient().when(orderMapper.updateById(any(RentalOrder.class))).thenReturn(1);
        lenient().when(historyMapper.insert(any(OrderStatusHistory.class))).thenReturn(1);

        RentalOrderResponse resp = service.freezeDeposit(1L, 10L, new BigDecimal("50.00"));

        assertThat(resp.getFrozenDepositAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(o.getFrozenDepositAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(o.getStatus()).isEqualTo(OrderStatusEnum.PENDING_PAYMENT.code());
        verify(walletClient).freezeDeposit(eq(1L), any(FreezeDepositFeignRequest.class));
    }

    @Test
    void freezeDeposit_fullyPaid_advancesStatus() {
        RentalOrder o = order(1L, OrderStatusEnum.PENDING_PAYMENT.code(),
                new BigDecimal("100.00"), new BigDecimal("50.00"));
        when(orderMapper.selectById(1L)).thenReturn(o);
        when(walletClient.prepayRent(eq(1L), any(PrepayRentFeignRequest.class)))
                .thenReturn(ApiResponse.success(new WalletOperationResponse(
                        1L, new BigDecimal("100.00"), BigDecimal.ZERO, false)));
        when(walletClient.freezeDeposit(eq(1L), any(FreezeDepositFeignRequest.class)))
                .thenReturn(ApiResponse.success(new WalletOperationResponse(
                        1L, new BigDecimal("100.00"), new BigDecimal("50.00"), true)));
        lenient().when(orderMapper.updateById(any(RentalOrder.class))).thenReturn(1);
        lenient().when(historyMapper.insert(any(OrderStatusHistory.class))).thenReturn(1);

        service.payOrder(1L, 10L, new BigDecimal("100.00"));
        service.freezeDeposit(1L, 10L, new BigDecimal("50.00"));

        assertThat(o.getStatus()).isEqualTo(OrderStatusEnum.PAID_PENDING_DELIVERY.code());
    }

    private RentalOrder order(Long id, Integer status, BigDecimal rentAmount, BigDecimal depositAmount) {
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
        o.setRentAmount(rentAmount);
        o.setDepositAmount(depositAmount);
        o.setStatus(status);
        return o;
    }
}
