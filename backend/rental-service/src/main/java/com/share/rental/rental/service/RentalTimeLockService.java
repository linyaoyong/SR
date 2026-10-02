package com.share.rental.rental.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.redis.RedisKey;
import com.share.rental.common.redis.RedisLockSupport;
import com.share.rental.rental.entity.RentalTimeLock;
import com.share.rental.rental.enums.TimeLockStatusEnum;
import com.share.rental.rental.mapper.RentalTimeLockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RentalTimeLockService {

    private static final Duration TIME_LOCK_TTL = Duration.ofSeconds(10);

    private final RentalTimeLockMapper timeLockMapper;
    private final RedisLockSupport redisLockSupport;

    @Autowired
    public RentalTimeLockService(RentalTimeLockMapper timeLockMapper, RedisLockSupport redisLockSupport) {
        this.timeLockMapper = timeLockMapper;
        this.redisLockSupport = redisLockSupport;
    }

    public void checkOverlap(Long itemId, LocalDateTime newStart, LocalDateTime newEnd,
                             Integer requestedQuantity, Integer itemQuantity) {
        redisLockSupport.runWithLock(timeLockKey(itemId), TIME_LOCK_TTL, () -> {
            checkOverlapLocked(itemId, newStart, newEnd, requestedQuantity, itemQuantity);
            return null;
        });
    }

    private void checkOverlapLocked(Long itemId, LocalDateTime newStart, LocalDateTime newEnd,
                                    Integer requestedQuantity, Integer itemQuantity) {
        QueryWrapper<RentalTimeLock> wrapper = new QueryWrapper<RentalTimeLock>()
                .eq("item_id", itemId)
                .eq("status", TimeLockStatusEnum.OCCUPIED.code())
                .last("FOR UPDATE");
        List<RentalTimeLock> occupied = timeLockMapper.selectList(wrapper);
        int sum = occupied.stream()
                .filter(lock -> lock.getRentStartTime().isBefore(newEnd)
                        && lock.getRentEndTime().isAfter(newStart))
                .mapToInt(RentalTimeLock::getQuantity)
                .sum();
        if (sum + requestedQuantity > itemQuantity) {
            throw new BusinessException(ErrorCode.RENTAL_TIME_STOCK_NOT_ENOUGH);
        }
    }

    public void createLock(Long orderId, Long itemId, Integer quantity,
                           LocalDateTime rentStartTime, LocalDateTime rentEndTime) {
        redisLockSupport.runWithLock(timeLockKey(itemId), TIME_LOCK_TTL, () -> {
            insertLock(orderId, itemId, quantity, rentStartTime, rentEndTime);
            return null;
        });
    }

    public void createLockIfAvailable(Long orderId, Long itemId, Integer quantity,
                                      LocalDateTime rentStartTime, LocalDateTime rentEndTime,
                                      Integer itemQuantity) {
        redisLockSupport.runWithLock(timeLockKey(itemId), TIME_LOCK_TTL, () -> {
            checkOverlapLocked(itemId, rentStartTime, rentEndTime, quantity, itemQuantity);
            insertLock(orderId, itemId, quantity, rentStartTime, rentEndTime);
            return null;
        });
    }

    public void releaseLocks(Long orderId) {
        UpdateWrapper<RentalTimeLock> wrapper = new UpdateWrapper<RentalTimeLock>()
                .eq("order_id", orderId)
                .eq("status", TimeLockStatusEnum.OCCUPIED.code())
                .set("status", TimeLockStatusEnum.RELEASED.code());
        timeLockMapper.update(null, wrapper);
    }

    private void insertLock(Long orderId, Long itemId, Integer quantity,
                            LocalDateTime rentStartTime, LocalDateTime rentEndTime) {
        RentalTimeLock lock = new RentalTimeLock();
        lock.setOrderId(orderId);
        lock.setItemId(itemId);
        lock.setQuantity(quantity);
        lock.setRentStartTime(rentStartTime);
        lock.setRentEndTime(rentEndTime);
        lock.setStatus(TimeLockStatusEnum.OCCUPIED.code());
        timeLockMapper.insert(lock);
    }

    private String timeLockKey(Long itemId) {
        return RedisKey.TIME_LOCK + itemId;
    }
}
