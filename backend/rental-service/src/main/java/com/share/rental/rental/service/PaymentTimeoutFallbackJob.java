package com.share.rental.rental.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.share.rental.common.redis.RedisKey;
import com.share.rental.common.redis.RedisLockSupport;
import com.share.rental.rental.config.PaymentTimeoutProperties;
import com.share.rental.rental.entity.RentalOrder;
import com.share.rental.rental.enums.OrderStatusEnum;
import com.share.rental.rental.mapper.RentalOrderMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentTimeoutFallbackJob {

    private static final String LOCK_KEY = RedisKey.SCHEDULER_LOCK + "payment-timeout";

    private final RedisLockSupport redisLockSupport;
    private final RentalOrderMapper orderMapper;
    private final RentalOrderService rentalOrderService;
    private final PaymentTimeoutProperties properties;

    public PaymentTimeoutFallbackJob(RedisLockSupport redisLockSupport,
                                     RentalOrderMapper orderMapper,
                                     RentalOrderService rentalOrderService,
                                     PaymentTimeoutProperties properties) {
        this.redisLockSupport = redisLockSupport;
        this.orderMapper = orderMapper;
        this.rentalOrderService = rentalOrderService;
        this.properties = properties;
    }

    @Scheduled(
            fixedDelayString = "${rental.payment.timeout-scan-ms:60000}",
            initialDelayString = "${rental.payment.timeout-scan-initial-delay-ms:60000}")
    public void scan() {
        redisLockSupport.runWithLock(LOCK_KEY, Duration.ofMillis(properties.getTimeoutScanMs()), () -> {
            doScan();
            return null;
        });
    }

    private void doScan() {
        LocalDateTime deadline = LocalDateTime.now().minus(properties.timeoutDuration());
        List<RentalOrder> orders = orderMapper.selectList(new QueryWrapper<RentalOrder>()
                .eq("status", OrderStatusEnum.PENDING_PAYMENT.code())
                .le("create_time", deadline));
        for (RentalOrder order : orders) {
            rentalOrderService.cancelPendingPaymentBySystem(order.getId(), "支付超时兜底取消");
        }
    }
}
