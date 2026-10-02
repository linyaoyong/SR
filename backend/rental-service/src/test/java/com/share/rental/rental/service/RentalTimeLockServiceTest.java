package com.share.rental.rental.service;

import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.redis.RedisLockSupport;
import com.share.rental.rental.entity.RentalTimeLock;
import com.share.rental.rental.enums.TimeLockStatusEnum;
import com.share.rental.rental.mapper.RentalTimeLockMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalTimeLockServiceTest {

    @Mock
    private RentalTimeLockMapper timeLockMapper;
    @Mock
    private RedisLockSupport redisLockSupport;

    @InjectMocks
    private RentalTimeLockService service;

    @org.junit.jupiter.api.BeforeEach
    void setUpRedisLock() {
        lenient().when(redisLockSupport.runWithLock(any(), any(), any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(2);
            return supplier.get();
        });
    }

    @Test
    void overlapUsesHalfOpenIntervals_adjacentSlotsDoNotOverlap() {
        // existing lock [10:00, 12:00), new request [12:00, 14:00) — adjacent, no overlap
        RentalTimeLock existing = lock(1L, 50L, 1,
                LocalDateTime.of(2026, 7, 1, 10, 0),
                LocalDateTime.of(2026, 7, 1, 12, 0));
        when(timeLockMapper.selectList(any())).thenReturn(List.of(existing));

        assertThatCode(() -> service.checkOverlap(50L,
                LocalDateTime.of(2026, 7, 1, 12, 0),
                LocalDateTime.of(2026, 7, 1, 14, 0),
                1, 1))
                .doesNotThrowAnyException();
    }

    @Test
    void overlapUsesHalfOpenIntervals_overlappingSlotsRejected() {
        // existing lock [10:00, 12:00), new request [11:59, 13:00) — overlaps
        RentalTimeLock existing = lock(1L, 50L, 1,
                LocalDateTime.of(2026, 7, 1, 10, 0),
                LocalDateTime.of(2026, 7, 1, 12, 0));
        when(timeLockMapper.selectList(any())).thenReturn(List.of(existing));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.checkOverlap(50L,
                        LocalDateTime.of(2026, 7, 1, 11, 59),
                        LocalDateTime.of(2026, 7, 1, 13, 0),
                        1, 1));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_TIME_STOCK_NOT_ENOUGH);
    }

    @Test
    void rejectsWhenOverlappingQuantityExceedsItemQuantity() {
        // item quantity = 2, existing occupied sum = 2, requested = 1 → 2+1 > 2 rejected
        RentalTimeLock first = lock(1L, 50L, 1,
                LocalDateTime.of(2026, 7, 1, 10, 0),
                LocalDateTime.of(2026, 7, 1, 14, 0));
        RentalTimeLock second = lock(2L, 50L, 1,
                LocalDateTime.of(2026, 7, 1, 11, 0),
                LocalDateTime.of(2026, 7, 1, 13, 0));
        when(timeLockMapper.selectList(any())).thenReturn(List.of(first, second));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.checkOverlap(50L,
                        LocalDateTime.of(2026, 7, 1, 12, 0),
                        LocalDateTime.of(2026, 7, 1, 13, 0),
                        1, 2));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_TIME_STOCK_NOT_ENOUGH);
    }

    @Test
    void allowsWhenQuantityFitsWithinItemQuantity() {
        // item quantity = 3, existing occupied sum = 2, requested = 1 → 2+1 <= 3 allowed
        RentalTimeLock first = lock(1L, 50L, 1,
                LocalDateTime.of(2026, 7, 1, 10, 0),
                LocalDateTime.of(2026, 7, 1, 14, 0));
        RentalTimeLock second = lock(2L, 50L, 1,
                LocalDateTime.of(2026, 7, 1, 11, 0),
                LocalDateTime.of(2026, 7, 1, 13, 0));
        when(timeLockMapper.selectList(any())).thenReturn(List.of(first, second));

        assertThatCode(() -> service.checkOverlap(50L,
                LocalDateTime.of(2026, 7, 1, 12, 0),
                LocalDateTime.of(2026, 7, 1, 13, 0),
                1, 3))
                .doesNotThrowAnyException();
    }

    @Test
    void ignoresReleasedLocks() {
        // released lock should not be counted
        RentalTimeLock released = lock(1L, 50L, 1,
                LocalDateTime.of(2026, 7, 1, 10, 0),
                LocalDateTime.of(2026, 7, 1, 12, 0));
        released.setStatus(TimeLockStatusEnum.RELEASED.code());
        when(timeLockMapper.selectList(any())).thenReturn(List.of());

        assertThatCode(() -> service.checkOverlap(50L,
                LocalDateTime.of(2026, 7, 1, 11, 0),
                LocalDateTime.of(2026, 7, 1, 13, 0),
                1, 1))
                .doesNotThrowAnyException();
    }

    @Test
    void createLock_insertsOccupiedLock() {
        when(timeLockMapper.insert(any(RentalTimeLock.class))).thenReturn(1);

        service.createLock(100L, 50L, 2,
                LocalDateTime.of(2026, 7, 1, 10, 0),
                LocalDateTime.of(2026, 7, 1, 12, 0));

        ArgumentCaptor<RentalTimeLock> captor = ArgumentCaptor.forClass(RentalTimeLock.class);
        verify(timeLockMapper).insert(captor.capture());
        RentalTimeLock saved = captor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(100L);
        assertThat(saved.getItemId()).isEqualTo(50L);
        assertThat(saved.getQuantity()).isEqualTo(2);
        assertThat(saved.getStatus()).isEqualTo(TimeLockStatusEnum.OCCUPIED.code());
        assertThat(saved.getRentStartTime()).isEqualTo(LocalDateTime.of(2026, 7, 1, 10, 0));
        assertThat(saved.getRentEndTime()).isEqualTo(LocalDateTime.of(2026, 7, 1, 12, 0));
        verify(redisLockSupport).runWithLock(any(), any(), any());
    }

    @Test
    void createLockIfAvailable_checksOverlapAndInsertsInSameRedisLock() {
        RentalTimeLock existing = lock(1L, 50L, 1,
                LocalDateTime.of(2026, 7, 1, 10, 0),
                LocalDateTime.of(2026, 7, 1, 12, 0));
        when(timeLockMapper.selectList(any())).thenReturn(List.of(existing));
        when(timeLockMapper.insert(any(RentalTimeLock.class))).thenReturn(1);

        service.createLockIfAvailable(100L, 50L, 1,
                LocalDateTime.of(2026, 7, 1, 12, 0),
                LocalDateTime.of(2026, 7, 1, 14, 0),
                1);

        verify(redisLockSupport).runWithLock(any(), any(), any());
        verify(timeLockMapper).selectList(any());
        verify(timeLockMapper).insert(any(RentalTimeLock.class));
    }

    @Test
    void createLockIfAvailable_rejectsWithoutInsertWhenQuantityExceeded() {
        RentalTimeLock existing = lock(1L, 50L, 1,
                LocalDateTime.of(2026, 7, 1, 10, 0),
                LocalDateTime.of(2026, 7, 1, 12, 0));
        when(timeLockMapper.selectList(any())).thenReturn(List.of(existing));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createLockIfAvailable(100L, 50L, 1,
                        LocalDateTime.of(2026, 7, 1, 11, 0),
                        LocalDateTime.of(2026, 7, 1, 13, 0),
                        1));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.RENTAL_TIME_STOCK_NOT_ENOUGH);
        verify(timeLockMapper, org.mockito.Mockito.never()).insert(any(RentalTimeLock.class));
    }

    @Test
    void checkOverlap_redisLockBusyRejectsWithoutMysqlQuery() {
        doThrow(new BusinessException(ErrorCode.REQUEST_TOO_FREQUENT))
                .when(redisLockSupport).runWithLock(any(), any(), any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.checkOverlap(50L,
                        LocalDateTime.of(2026, 7, 1, 10, 0),
                        LocalDateTime.of(2026, 7, 1, 12, 0),
                        1, 1));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.REQUEST_TOO_FREQUENT);
        verify(timeLockMapper, org.mockito.Mockito.never()).selectList(any());
    }

    @Test
    void releaseLocks_marksLocksAsReleased() {
        lenient().when(timeLockMapper.update(isNull(), any())).thenReturn(1);

        service.releaseLocks(100L);

        verify(timeLockMapper).update(isNull(), any());
    }

    private RentalTimeLock lock(Long id, Long itemId, Integer quantity,
                                LocalDateTime start, LocalDateTime end) {
        RentalTimeLock lock = new RentalTimeLock();
        lock.setId(id);
        lock.setOrderId(100L);
        lock.setItemId(itemId);
        lock.setQuantity(quantity);
        lock.setRentStartTime(start);
        lock.setRentEndTime(end);
        lock.setStatus(TimeLockStatusEnum.OCCUPIED.code());
        return lock;
    }
}
