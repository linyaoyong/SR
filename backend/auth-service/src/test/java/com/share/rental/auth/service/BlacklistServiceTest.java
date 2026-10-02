package com.share.rental.auth.service;

import com.share.rental.auth.dto.BlacklistResponse;
import com.share.rental.auth.entity.User;
import com.share.rental.auth.entity.UserBlacklist;
import com.share.rental.auth.mapper.UserBlacklistMapper;
import com.share.rental.auth.mapper.UserMapper;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlacklistServiceTest {

    @Mock
    private UserBlacklistMapper blacklistMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private BlacklistService blacklistService;

    @Test
    void blacklist_self_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> blacklistService.blacklist(1L, 1L, "no"));
        assertEquals(ErrorCode.AUTH_CANNOT_BLACKLIST_SELF.code(), ex.errorCode().code());
        verify(blacklistMapper, never()).insert(any(UserBlacklist.class));
    }

    @Test
    void blacklist_targetNotExist_throws() {
        when(userMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> blacklistService.blacklist(1L, 99L, "reason"));
        assertEquals(ErrorCode.AUTH_USER_NOT_FOUND.code(), ex.errorCode().code());
    }

    @Test
    void blacklist_alreadyExist_throws() {
        User target = new User();
        target.setId(2L);
        when(userMapper.selectById(2L)).thenReturn(target);
        when(blacklistMapper.selectOne(any())).thenReturn(new UserBlacklist());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> blacklistService.blacklist(1L, 2L, "reason"));
        assertEquals(ErrorCode.AUTH_BLACKLIST_EXISTS.code(), ex.errorCode().code());
    }

    @Test
    void blacklist_success() {
        User target = new User();
        target.setId(2L);
        target.setUsername("bob");
        when(userMapper.selectById(2L)).thenReturn(target);
        when(blacklistMapper.selectOne(any())).thenReturn(null);

        blacklistService.blacklist(1L, 2L, "reason");

        verify(blacklistMapper).insert(any(UserBlacklist.class));
    }

    @Test
    void unblacklist_notExist_throws() {
        when(blacklistMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> blacklistService.unblacklist(1L, 99L));
        assertEquals(ErrorCode.AUTH_BLACKLIST_NOT_FOUND.code(), ex.errorCode().code());
    }

    @Test
    void unblacklist_success() {
        UserBlacklist record = new UserBlacklist();
        record.setId(5L);
        when(blacklistMapper.selectOne(any())).thenReturn(record);

        blacklistService.unblacklist(1L, 2L);

        verify(blacklistMapper).deleteById(5L);
    }

    @Test
    void listBlacklist_returnsResponses() {
        UserBlacklist record = new UserBlacklist();
        record.setId(5L);
        record.setUserId(1L);
        record.setTargetUserId(2L);
        record.setReason("reason");
        User target = new User();
        target.setId(2L);
        target.setUsername("bob");
        when(blacklistMapper.selectList(any())).thenReturn(List.of(record));
        when(userMapper.selectById(2L)).thenReturn(target);

        List<BlacklistResponse> result = blacklistService.listBlacklist(1L);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getTargetUserId());
        assertEquals("bob", result.get(0).getTargetUsername());
    }
}
