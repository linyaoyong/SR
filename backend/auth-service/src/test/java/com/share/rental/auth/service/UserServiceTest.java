package com.share.rental.auth.service;

import com.share.rental.auth.dto.LoginRequest;
import com.share.rental.auth.dto.LoginResponse;
import com.share.rental.auth.dto.RefreshResponse;
import com.share.rental.auth.dto.RegisterRequest;
import com.share.rental.auth.dto.CreditScoreAdjustRequest;
import com.share.rental.auth.dto.UserMeResponse;
import com.share.rental.auth.entity.CreditScoreRecord;
import com.share.rental.auth.entity.User;
import com.share.rental.auth.mapper.CreditScoreRecordMapper;
import com.share.rental.auth.mapper.UserMapper;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.security.JwtClaims;
import com.share.rental.common.security.JwtUtil;
import com.share.rental.common.security.SecurityProperties;
import com.share.rental.common.upload.ImageUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private CreditScoreRecordMapper creditScoreRecordMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SecurityProperties securityProperties;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private ImageUploadService imageUploadService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        lenient().when(securityProperties.getJwtSecret()).thenReturn("test-only-jwt-secret-at-least-32-bytes");
        lenient().when(securityProperties.getAccessTokenTtl()).thenReturn(Duration.ofHours(2));
    }

    @Test
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setPassword("secret123");
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$encodedHash");

        assertDoesNotThrow(() -> userService.register(req));
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void register_usernameExists_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("bob");
        req.setPassword("secret123");
        User existing = new User();
        existing.setId(1L);
        when(userMapper.selectOne(any())).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.register(req));
        assertEquals(ErrorCode.AUTH_USERNAME_EXISTS.code(), ex.errorCode().code());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void login_success_returnsUserToken() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("secret123");
        User user = new User();
        user.setId(10L);
        user.setUsername("alice");
        user.setPassword("$2a$10$encodedHash");
        user.setRole(0);
        user.setStatus(0);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("secret123", "$2a$10$encodedHash")).thenReturn(true);
        when(jwtUtil.createToken(eq(10L), eq("alice"), eq("USER"), any(Duration.class))).thenReturn("token-abc");
        when(refreshTokenService.issueRefreshToken(10L, "alice", "USER")).thenReturn("refresh-abc");

        LoginResponse resp = userService.login(req);

        assertEquals(10L, resp.getUserId());
        assertEquals("alice", resp.getUsername());
        assertEquals("USER", resp.getRole());
        assertEquals("token-abc", resp.getAccessToken());
        assertEquals("refresh-abc", resp.getRefreshToken());
        verify(userMapper).updateById(ArgumentMatchers.<User>argThat(u -> u.getId().equals(10L) && u.getLastLoginTime() != null));
    }

    @Test
    void login_badCredentials_throws() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("wrong");
        User user = new User();
        user.setId(10L);
        user.setPassword("$2a$10$encodedHash");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("wrong", "$2a$10$encodedHash")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(req));
        assertEquals(ErrorCode.AUTH_BAD_CREDENTIALS.code(), ex.errorCode().code());
    }

    @Test
    void login_banned_throws() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("secret123");
        User user = new User();
        user.setId(10L);
        user.setUsername("alice");
        user.setPassword("$2a$10$encodedHash");
        user.setRole(0);
        user.setStatus(1);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("secret123", "$2a$10$encodedHash")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(req));

        assertEquals(ErrorCode.AUTH_ACCOUNT_BANNED.code(), ex.errorCode().code());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void adminLogin_nonAdmin_throws() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("secret123");
        User user = new User();
        user.setId(10L);
        user.setPassword("$2a$10$encodedHash");
        user.setRole(0);
        user.setStatus(0);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("secret123", "$2a$10$encodedHash")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.adminLogin(req));
        assertEquals(ErrorCode.AUTH_NOT_ADMIN.code(), ex.errorCode().code());
    }

    @Test
    void adminLogin_success_returnsAdminToken() {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("123456");
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("$2a$10$encodedHash");
        user.setRole(1);
        user.setStatus(0);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("123456", "$2a$10$encodedHash")).thenReturn(true);
        when(jwtUtil.createToken(eq(1L), eq("admin"), eq("ADMIN"), any(Duration.class))).thenReturn("admin-token");
        when(refreshTokenService.issueRefreshToken(1L, "admin", "ADMIN")).thenReturn("admin-refresh");

        LoginResponse resp = userService.adminLogin(req);

        assertEquals("ADMIN", resp.getRole());
        assertEquals("admin-token", resp.getAccessToken());
        assertEquals("admin-refresh", resp.getRefreshToken());
    }

    @Test
    void getMe_existingUser_returnsResponse() {
        User user = new User();
        user.setId(10L);
        user.setUsername("alice");
        user.setRole(0);
        user.setStatus(0);
        user.setCreditScore(100);
        when(userMapper.selectById(10L)).thenReturn(user);

        UserMeResponse resp = userService.getMe(10L);

        assertEquals(10L, resp.getId());
        assertEquals("alice", resp.getUsername());
    }

    @Test
    void getMe_notFound_throws() {
        when(userMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.getMe(99L));
        assertEquals(ErrorCode.AUTH_USER_NOT_FOUND.code(), ex.errorCode().code());
    }

    @Test
    void refresh_validToken_returnsNewToken() {
        when(securityProperties.getAccessTokenTtl()).thenReturn(Duration.ofHours(2));
        when(refreshTokenService.rotate("valid-token")).thenReturn(
                new RefreshTokenService.RefreshTokenPayload(10L, "alice", "USER"));
        User user = new User();
        user.setId(10L); user.setUsername("alice"); user.setRole(0); user.setStatus(0);
        when(userMapper.selectById(10L)).thenReturn(user);
        when(refreshTokenService.issueRefreshToken(10L, "alice", "USER")).thenReturn("new-refresh");
        when(jwtUtil.createToken(eq(10L), eq("alice"), eq("USER"), any(Duration.class))).thenReturn("new-token");

        RefreshResponse resp = userService.refresh("Bearer valid-token");

        assertEquals("new-token", resp.getAccessToken());
        assertEquals("new-refresh", resp.getRefreshToken());
    }

    @Test
    void refresh_bannedUserCannotReceiveAnyNewToken() {
        when(refreshTokenService.rotate("old-token")).thenReturn(
                new RefreshTokenService.RefreshTokenPayload(10L, "old", "ADMIN"));
        User user = new User();
        user.setId(10L);
        user.setStatus(1);
        lenient().when(userMapper.selectById(10L)).thenReturn(user);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.refresh("Bearer old-token"));
        assertEquals(ErrorCode.AUTH_ACCOUNT_BANNED, ex.errorCode());
        verifyNoInteractions(jwtUtil);
        verify(refreshTokenService, never()).issueRefreshToken(any(), any(), any());
    }

    @Test
    void refresh_downgradedUserUsesCurrentUsernameAndRoleInBothTokens() {
        when(refreshTokenService.rotate("old-token")).thenReturn(
                new RefreshTokenService.RefreshTokenPayload(10L, "old-admin", "ADMIN"));
        User user = new User();
        user.setId(10L);
        user.setUsername("current-user");
        user.setRole(0);
        user.setStatus(0);
        lenient().when(userMapper.selectById(10L)).thenReturn(user);
        JwtUtil realJwt = new JwtUtil("test-only-refresh-signing-key-at-least-32-bytes");
        org.springframework.test.util.ReflectionTestUtils.setField(userService, "jwtUtil", realJwt);
        lenient().when(refreshTokenService.issueRefreshToken(10L, "current-user", "USER"))
                .thenReturn("current-refresh");
        RefreshResponse response = userService.refresh("Bearer old-token");
        JwtClaims claims = realJwt.parse(response.getAccessToken());
        assertEquals(10L, claims.userId());
        assertEquals("current-user", claims.username());
        assertEquals("USER", claims.role());
        assertEquals("current-refresh", response.getRefreshToken());
        verify(refreshTokenService).issueRefreshToken(10L, "current-user", "USER");
    }

    @Test
    void refresh_deletedUserDoesNotIssueSuccessor() {
        when(refreshTokenService.rotate("deleted-token")).thenReturn(
                new RefreshTokenService.RefreshTokenPayload(10L, "old", "ADMIN"));
        when(userMapper.selectById(10L)).thenReturn(null);
        BusinessException error = assertThrows(BusinessException.class,
                () -> userService.refresh("Bearer deleted-token"));
        assertEquals(ErrorCode.AUTH_USER_NOT_FOUND, error.errorCode());
        verifyNoInteractions(jwtUtil);
        verify(refreshTokenService, never()).issueRefreshToken(any(), any(), any());
    }

    @Test
    void refresh_invalidHeader_throws() {
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.refresh(null));
        assertEquals(ErrorCode.AUTH_REFRESH_TOKEN_INVALID.code(), ex.errorCode().code());
    }

    @Test
    void adjustCreditScore_updatesUserAndWritesRecord() {
        User user = new User();
        user.setId(10L);
        user.setCreditScore(100);
        when(userMapper.selectById(10L)).thenReturn(user);

        CreditScoreAdjustRequest request = new CreditScoreAdjustRequest();
        request.setOrderId(99L);
        request.setChangeValue(1);
        request.setReasonType("ORDER_COMPLETED");
        request.setReason("完成订单");

        userService.adjustCreditScore(10L, request);

        verify(userMapper).updateById(ArgumentMatchers.<User>argThat(updated ->
                updated.getId().equals(10L) && Integer.valueOf(101).equals(updated.getCreditScore())));
        verify(creditScoreRecordMapper).insert(argThat((CreditScoreRecord record) ->
                record.getUserId().equals(10L)
                        && record.getOrderId().equals(99L)
                        && Integer.valueOf(1).equals(record.getChangeValue())
                        && Integer.valueOf(100).equals(record.getBeforeScore())
                        && Integer.valueOf(101).equals(record.getAfterScore())
                        && "ORDER_COMPLETED".equals(record.getReasonType())));
    }
}
