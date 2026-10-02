package com.share.rental.rental.service;

import com.share.rental.common.redis.RedisLockSupport;
import com.share.rental.rental.config.PaymentTimeoutProperties;
import com.share.rental.rental.entity.RentalOrder;
import com.share.rental.rental.enums.OrderStatusEnum;
import com.share.rental.rental.mapper.RentalOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentTimeoutFallbackJobTest {

    @Mock
    private RedisLockSupport redisLockSupport;
    @Mock
    private RentalOrderMapper orderMapper;
    @Mock
    private RentalOrderService rentalOrderService;

    private PaymentTimeoutFallbackJob job;

    @BeforeEach
    void setUp() {
        when(redisLockSupport.runWithLock(any(), any(), any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(2);
            return supplier.get();
        });
        PaymentTimeoutProperties properties = new PaymentTimeoutProperties();
        properties.setTimeoutMinutes(30);
        properties.setTimeoutScanMs(60000);
        job = new PaymentTimeoutFallbackJob(redisLockSupport, orderMapper, rentalOrderService, properties);
    }

    @Test
    void scan_cancelsExpiredPendingPaymentOrdersUnderSchedulerLock() {
        RentalOrder first = order(1L);
        RentalOrder second = order(2L);
        when(orderMapper.selectList(any())).thenReturn(List.of(first, second));

        job.scan();

        verify(redisLockSupport).runWithLock(any(), any(), any());
        verify(rentalOrderService).cancelPendingPaymentBySystem(1L, "支付超时兜底取消");
        verify(rentalOrderService).cancelPendingPaymentBySystem(2L, "支付超时兜底取消");
    }

    private RentalOrder order(Long id) {
        RentalOrder order = new RentalOrder();
        order.setId(id);
        order.setStatus(OrderStatusEnum.PENDING_PAYMENT.code());
        return order;
    }
}
