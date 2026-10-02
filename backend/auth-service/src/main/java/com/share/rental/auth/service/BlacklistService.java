package com.share.rental.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.rental.auth.dto.BlacklistResponse;
import com.share.rental.auth.entity.User;
import com.share.rental.auth.entity.UserBlacklist;
import com.share.rental.auth.mapper.UserBlacklistMapper;
import com.share.rental.auth.mapper.UserMapper;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BlacklistService {

    private final UserBlacklistMapper blacklistMapper;
    private final UserMapper userMapper;

    @Autowired
    public BlacklistService(UserBlacklistMapper blacklistMapper, UserMapper userMapper) {
        this.blacklistMapper = blacklistMapper;
        this.userMapper = userMapper;
    }

    public void blacklist(Long userId, Long targetUserId, String reason) {
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.AUTH_CANNOT_BLACKLIST_SELF);
        }
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND);
        }
        UserBlacklist existing = blacklistMapper.selectOne(new LambdaQueryWrapper<UserBlacklist>()
                .eq(UserBlacklist::getUserId, userId)
                .eq(UserBlacklist::getTargetUserId, targetUserId));
        if (existing != null) {
            throw new BusinessException(ErrorCode.AUTH_BLACKLIST_EXISTS);
        }
        UserBlacklist record = new UserBlacklist();
        record.setUserId(userId);
        record.setTargetUserId(targetUserId);
        record.setReason(reason);
        blacklistMapper.insert(record);
    }

    public void unblacklist(Long userId, Long targetUserId) {
        UserBlacklist record = blacklistMapper.selectOne(new LambdaQueryWrapper<UserBlacklist>()
                .eq(UserBlacklist::getUserId, userId)
                .eq(UserBlacklist::getTargetUserId, targetUserId));
        if (record == null) {
            throw new BusinessException(ErrorCode.AUTH_BLACKLIST_NOT_FOUND);
        }
        blacklistMapper.deleteById(record.getId());
    }

    public List<BlacklistResponse> listBlacklist(Long userId) {
        List<UserBlacklist> records = blacklistMapper.selectList(new LambdaQueryWrapper<UserBlacklist>()
                .eq(UserBlacklist::getUserId, userId)
                .orderByDesc(UserBlacklist::getCreateTime));
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream().map(record -> {
            User target = userMapper.selectById(record.getTargetUserId());
            String targetName = target != null ? target.getUsername() : null;
            return new BlacklistResponse(
                    record.getId(),
                    record.getTargetUserId(),
                    targetName,
                    record.getReason(),
                    record.getCreateTime()
            );
        }).collect(Collectors.toList());
    }
}
