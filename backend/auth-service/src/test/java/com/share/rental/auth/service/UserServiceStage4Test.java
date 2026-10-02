package com.share.rental.auth.service;

import com.share.rental.auth.dto.BanUserRequest;
import com.share.rental.auth.dto.FileUploadResponse;
import com.share.rental.auth.dto.RegisterRequest;
import com.share.rental.auth.dto.UpdatePasswordRequest;
import com.share.rental.auth.dto.UpdateUserProfileRequest;
import com.share.rental.auth.dto.UserAuditItemResponse;
import com.share.rental.auth.dto.UserAuditRequest;
import com.share.rental.auth.dto.UserMeResponse;
import com.share.rental.auth.dto.UserPublicResponse;
import com.share.rental.auth.entity.User;
import com.share.rental.auth.mapper.UserMapper;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.security.JwtUtil;
import com.share.rental.common.security.SecurityProperties;
import com.share.rental.common.upload.ImageUploadService;
import com.share.rental.common.upload.UploadedFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceStage4Test {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SecurityProperties securityProperties;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private ImageUploadService imageUploadService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        lenient().when(securityProperties.getAccessTokenTtl()).thenReturn(Duration.ofHours(2));
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("$2a$10$encodedHash");
        user.setStatus(0);
        user.setRole(0);
        user.setCreditScore(100);
        user.setUsernameAuditStatus(0);
        user.setAvatarAuditStatus(0);
        user.setDescriptionAuditStatus(0);
        user.setShowRentalHistory(0);
        return user;
    }

    @Test
    void register_marksOnlyUsernamePendingWhenAvatarAndDescriptionAreEmpty() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("secret1")).thenReturn("$2a$10$encodedHash");

        RegisterRequest request = new RegisterRequest();
        request.setUsername("new-user");
        request.setPassword("secret1");

        userService.register(request);

        verify(userMapper).insert(ArgumentMatchers.<User>argThat(u ->
                "new-user".equals(u.getUsername())
                        && u.getUsernameAuditStatus() == 0
                        && u.getAvatarAuditStatus() == 1
                        && u.getDescriptionAuditStatus() == 1));
    }

    @Test
    void updateProfile_bannedUser_rejected() {
        User banned = user(7L, "banned");
        banned.setStatus(1);
        when(userMapper.selectById(7L)).thenReturn(banned);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateProfile(7L, new UpdateUserProfileRequest("newName", "desc", 1)));
        assertThat(ex.errorCode()).isEqualTo(ErrorCode.AUTH_ACCOUNT_BANNED);
    }

    @Test
    void updateProfile_changesUsername_resetsUsernameAuditStatus() {
        User user = user(1L, "oldName");
        user.setUsernameAuditStatus(1);
        when(userMapper.selectById(1L)).thenReturn(user);

        userService.updateProfile(1L, new UpdateUserProfileRequest("newName", null, null));

        verify(userMapper).updateById(ArgumentMatchers.<User>argThat(u ->
                "newName".equals(u.getUsername()) && u.getUsernameAuditStatus() == 0));
    }

    @Test
    void updateProfile_changesDescription_resetsDescriptionAuditStatus() {
        User user = user(1L, "alice");
        user.setDescriptionAuditStatus(1);
        when(userMapper.selectById(1L)).thenReturn(user);

        userService.updateProfile(1L, new UpdateUserProfileRequest(null, "new desc", null));

        verify(userMapper).updateById(ArgumentMatchers.<User>argThat(u ->
                "new desc".equals(u.getDescription()) && u.getDescriptionAuditStatus() == 0));
    }

    @Test
    void updateProfile_changesShowRentalHistory_updatesField() {
        User user = user(1L, "alice");
        when(userMapper.selectById(1L)).thenReturn(user);

        userService.updateProfile(1L, new UpdateUserProfileRequest(null, null, 1));

        verify(userMapper).updateById(ArgumentMatchers.<User>argThat(u ->
                u.getShowRentalHistory() == 1));
    }

    @Test
    void updateProfile_returnsUpdatedUserMeResponse() {
        User user = user(1L, "alice");
        when(userMapper.selectById(1L)).thenReturn(user);

        UserMeResponse resp = userService.updateProfile(1L,
                new UpdateUserProfileRequest("alice2", "hi", 1));

        assertThat(resp.getUsername()).isEqualTo("alice2");
        assertThat(resp.getDescription()).isEqualTo("hi");
        assertThat(resp.getShowRentalHistory()).isEqualTo(1);
    }

    @Test
    void auditUserField_rectifyDescription_publicProfileUsesDefaultDescription() {
        User user = user(8L, "森鲸");
        user.setDescription("bad desc");
        user.setDescriptionAuditStatus(2);
        when(userMapper.selectById(8L)).thenReturn(user);
        UserPublicResponse response = userService.getPublic(8L);
        assertThat(response.getDescription()).isEqualTo("这个人还没有填写简介");
    }

    @Test
    void getPublic_usernameRectified_returnsDefaultUsername() {
        User user = user(8L, "badname");
        user.setUsernameAuditStatus(2);
        when(userMapper.selectById(8L)).thenReturn(user);
        UserPublicResponse response = userService.getPublic(8L);
        assertThat(response.getUsername()).isEqualTo("邻享用户");
    }

    @Test
    void getPublic_avatarRectified_returnsDefaultAvatar() {
        User user = user(8L, "alice");
        user.setAvatarUrl("/files/avatars/x.jpg");
        user.setAvatarAuditStatus(2);
        when(userMapper.selectById(8L)).thenReturn(user);
        UserPublicResponse response = userService.getPublic(8L);
        assertThat(response.getAvatarUrl()).isEqualTo("");
    }

    @Test
    void getPublic_allApproved_returnsActualValues() {
        User user = user(8L, "alice");
        user.setAvatarUrl("/files/avatars/a.jpg");
        user.setDescription("hello");
        user.setUsernameAuditStatus(1);
        user.setAvatarAuditStatus(1);
        user.setDescriptionAuditStatus(1);
        when(userMapper.selectById(8L)).thenReturn(user);
        UserPublicResponse response = userService.getPublic(8L);
        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getAvatarUrl()).isEqualTo("/files/avatars/a.jpg");
        assertThat(response.getDescription()).isEqualTo("hello");
    }

    @Test
    void updateAvatar_callsImageUploadService_resetsAvatarAuditStatus() {
        User user = user(1L, "alice");
        user.setAvatarAuditStatus(1);
        when(userMapper.selectById(1L)).thenReturn(user);
        MultipartFile file = mock(MultipartFile.class);
        UploadedFile uploaded = new UploadedFile("/files/avatars/abc.jpg", "abc.jpg", "image/jpeg", 1024);
        when(imageUploadService.storeImage(eq(file), eq("avatars"))).thenReturn(uploaded);

        FileUploadResponse resp = userService.updateAvatar(1L, file);

        assertThat(resp.getUrl()).isEqualTo("/files/avatars/abc.jpg");
        assertThat(resp.getFilename()).isEqualTo("abc.jpg");
        assertThat(resp.getSize()).isEqualTo(1024L);
        verify(userMapper).updateById(ArgumentMatchers.<User>argThat(u ->
                "/files/avatars/abc.jpg".equals(u.getAvatarUrl()) && u.getAvatarAuditStatus() == 0));
    }

    @Test
    void updatePassword_bannedUser_rejected() {
        User banned = user(1L, "alice");
        banned.setStatus(1);
        when(userMapper.selectById(1L)).thenReturn(banned);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updatePassword(1L, new UpdatePasswordRequest("old", "newpass")));
        assertThat(ex.errorCode()).isEqualTo(ErrorCode.AUTH_ACCOUNT_BANNED);
    }

    @Test
    void updatePassword_wrongOldPassword_throws() {
        User user = user(1L, "alice");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("wrong", "$2a$10$encodedHash")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updatePassword(1L, new UpdatePasswordRequest("wrong", "newpass")));
        assertThat(ex.errorCode()).isEqualTo(ErrorCode.AUTH_BAD_CREDENTIALS);
    }

    @Test
    void updatePassword_success_encodesNewPassword() {
        User user = user(1L, "alice");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("old", "$2a$10$encodedHash")).thenReturn(true);
        when(passwordEncoder.encode("newpass")).thenReturn("$2a$10$newHash");

        userService.updatePassword(1L, new UpdatePasswordRequest("old", "newpass"));

        verify(userMapper).updateById(ArgumentMatchers.<User>argThat(u ->
                "$2a$10$newHash".equals(u.getPassword())));
    }

    @Test
    void banUser_setsStatusToBanned() {
        User user = user(1L, "alice");
        user.setStatus(0);
        when(userMapper.selectById(1L)).thenReturn(user);

        userService.banUser(100L, 1L, new BanUserRequest("spam"));

        verify(userMapper).updateById(ArgumentMatchers.<User>argThat(u -> u.getStatus() == 1));
    }

    @Test
    void unbanUser_setsStatusToActive() {
        User user = user(1L, "alice");
        user.setStatus(1);
        when(userMapper.selectById(1L)).thenReturn(user);

        userService.unbanUser(100L, 1L);

        verify(userMapper).updateById(ArgumentMatchers.<User>argThat(u -> u.getStatus() == 0));
    }

    @Test
    void auditUser_approveUsername_setsUsernameAuditStatusToApproved() {
        User user = user(1L, "alice");
        user.setUsernameAuditStatus(0);
        when(userMapper.selectById(1L)).thenReturn(user);

        userService.auditUser(100L, 1L, new UserAuditRequest("username", 1, null));

        verify(userMapper).updateById(ArgumentMatchers.<User>argThat(u -> u.getUsernameAuditStatus() == 1));
    }

    @Test
    void auditUser_rectifyDescription_setsDescriptionAuditStatusToRectify() {
        User user = user(1L, "alice");
        user.setDescriptionAuditStatus(0);
        when(userMapper.selectById(1L)).thenReturn(user);

        userService.auditUser(100L, 1L, new UserAuditRequest("description", 2, "inappropriate"));

        verify(userMapper).updateById(ArgumentMatchers.<User>argThat(u ->
                u.getDescriptionAuditStatus() == 2));
    }

    @Test
    void auditUser_rectifyWithoutReason_throws() {
        User user = user(1L, "alice");
        when(userMapper.selectById(1L)).thenReturn(user);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.auditUser(100L, 1L, new UserAuditRequest("description", 2, null)));
        assertThat(ex.errorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void auditUser_invalidFieldName_throws() {
        User user = user(1L, "alice");
        when(userMapper.selectById(1L)).thenReturn(user);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.auditUser(100L, 1L, new UserAuditRequest("invalid", 1, null)));
        assertThat(ex.errorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void auditUser_invalidAuditStatus_throws() {
        User user = user(1L, "alice");
        when(userMapper.selectById(1L)).thenReturn(user);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.auditUser(100L, 1L, new UserAuditRequest("username", 5, null)));
        assertThat(ex.errorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void listUserAudits_returnsOneRowPerMatchingUserAndSkipsEmptyAvatarDescription() {
        User u1 = user(1L, "alice");
        u1.setUsernameAuditStatus(0);
        u1.setAvatarAuditStatus(1);
        u1.setDescriptionAuditStatus(1);
        u1.setAvatarUrl("https://example.com/a.png");
        u1.setDescription("hello");
        User u2 = user(2L, "bob");
        u2.setUsernameAuditStatus(1);
        u2.setAvatarAuditStatus(0);
        u2.setDescriptionAuditStatus(0);
        when(userMapper.selectList(any())).thenReturn(List.of(u1, u2));

        List<UserAuditItemResponse> items = userService.listUserAudits(0);

        assertThat(items).hasSize(1);
        assertThat(items).extracting(UserAuditItemResponse::getFieldName)
                .containsExactly("username");
        assertThat(items).extracting(UserAuditItemResponse::getUserId)
                .containsExactly(1L);
        // 扩展字段应被透传到响应中
        UserAuditItemResponse aliceUsernameItem = items.stream()
                .filter(i -> i.getUserId() == 1L && "username".equals(i.getFieldName()))
                .findFirst().orElseThrow();
        assertThat(aliceUsernameItem.getAvatarUrl()).isEqualTo("https://example.com/a.png");
        assertThat(aliceUsernameItem.getDescription()).isEqualTo("hello");
        assertThat(aliceUsernameItem.getCreditScore()).isEqualTo(100);
        assertThat(aliceUsernameItem.getUsernameAuditStatus()).isEqualTo(0);
        assertThat(aliceUsernameItem.getAvatarAuditStatus()).isEqualTo(1);
    }

    @Test
    void updateProfile_duplicateUsername_throws() {
        User user = user(1L, "alice");
        when(userMapper.selectById(1L)).thenReturn(user);
        User existing = user(2L, "newName");
        lenient().when(userMapper.selectOne(any())).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateProfile(1L, new UpdateUserProfileRequest("newName", null, null)));
        assertThat(ex.errorCode()).isEqualTo(ErrorCode.AUTH_USERNAME_EXISTS);
    }

    @Test
    void updateProfile_sameUsername_doesNotResetAuditStatus() {
        User user = user(1L, "alice");
        user.setUsernameAuditStatus(1);
        when(userMapper.selectById(1L)).thenReturn(user);

        userService.updateProfile(1L, new UpdateUserProfileRequest("alice", null, null));

        verify(userMapper).updateById(ArgumentMatchers.<User>argThat(u ->
                u.getUsernameAuditStatus() == 1));
    }

    @Test
    void updateProfile_sameDescription_doesNotResetAuditStatus() {
        User user = user(1L, "alice");
        user.setDescription("hello");
        user.setDescriptionAuditStatus(1);
        when(userMapper.selectById(1L)).thenReturn(user);

        userService.updateProfile(1L, new UpdateUserProfileRequest(null, "hello", null));

        verify(userMapper).updateById(ArgumentMatchers.<User>argThat(u ->
                u.getDescriptionAuditStatus() == 1));
    }

    @Test
    void listUserAudits_nullAuditStatus_returnsOneRowPerPendingOrRectifyUser() {
        // auditStatus==null 表示"全部"，返回所有处于待审核(0)或要求整改(2)的用户；
        // 每个用户只出现一行，避免管理端列表把用户名、头像、简介拆成重复用户行。
        User pending = user(1L, "alice");
        pending.setAvatarAuditStatus(1);
        pending.setDescriptionAuditStatus(1);
        User rectify = user(2L, "bob");
        rectify.setUsernameAuditStatus(2);
        rectify.setAvatarAuditStatus(2);
        rectify.setDescriptionAuditStatus(1);
        User allApproved = user(3L, "carol");
        allApproved.setUsernameAuditStatus(1);
        allApproved.setAvatarAuditStatus(1);
        allApproved.setDescriptionAuditStatus(1);
        when(userMapper.selectList(any())).thenReturn(List.of(pending, rectify, allApproved));

        List<UserAuditItemResponse> items = userService.listUserAudits(null);

        assertThat(items).hasSize(2);
        assertThat(items).extracting(UserAuditItemResponse::getUserId)
                .containsExactlyInAnyOrder(1L, 2L);
        assertThat(items).filteredOn(i -> i.getUserId() == 2L)
                .extracting(UserAuditItemResponse::getFieldName)
                .containsExactly("username");
    }

    @Test
    void listUserAudits_fieldNamesContainsAllPendingFieldsForUser() {
        User u = user(1L, "alice");
        u.setUsernameAuditStatus(0);
        u.setAvatarAuditStatus(0);
        u.setDescriptionAuditStatus(1); // 已通过，不在待审列表
        u.setAvatarUrl("https://example.com/a.png");
        u.setDescription("hello");
        when(userMapper.selectList(any())).thenReturn(List.of(u));

        List<UserAuditItemResponse> items = userService.listUserAudits(0);

        assertThat(items).hasSize(1);
        UserAuditItemResponse item = items.get(0);
        assertThat(item.getFieldNames()).containsExactly("username", "avatar");
        assertThat(item.getAuditStatuses()).containsExactly(0, 0);
        // 首选字段仍用于操作
        assertThat(item.getFieldName()).isEqualTo("username");
        assertThat(item.getAuditStatus()).isEqualTo(0);
    }

    @Test
    void listUserAudits_nullAuditStatus_fieldNamesIncludesPendingAndRectify() {
        User u = user(1L, "alice");
        u.setUsernameAuditStatus(2);   // 整改
        u.setAvatarAuditStatus(0);     // 待审
        u.setDescriptionAuditStatus(1);// 已通过
        u.setAvatarUrl("https://example.com/a.png");
        u.setDescription("hello");
        when(userMapper.selectList(any())).thenReturn(List.of(u));

        List<UserAuditItemResponse> items = userService.listUserAudits(null);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getFieldNames()).containsExactly("username", "avatar");
        assertThat(items.get(0).getAuditStatuses()).containsExactly(2, 0);
    }

    @Test
    void banUser_adminTarget_throws() {
        User admin = user(1L, "admin");
        admin.setRole(1);
        when(userMapper.selectById(1L)).thenReturn(admin);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.banUser(100L, 1L, new BanUserRequest("spam")));
        assertThat(ex.errorCode()).isEqualTo(ErrorCode.ADMIN_NOT_ALLOWED);
    }
}
