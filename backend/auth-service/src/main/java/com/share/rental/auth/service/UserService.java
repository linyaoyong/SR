package com.share.rental.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.rental.auth.dto.BanUserRequest;
import com.share.rental.auth.dto.CreditScoreAdjustRequest;
import com.share.rental.auth.dto.FileUploadResponse;
import com.share.rental.auth.dto.LoginRequest;
import com.share.rental.auth.dto.LoginResponse;
import com.share.rental.auth.dto.RegisterRequest;
import com.share.rental.auth.dto.RefreshResponse;
import com.share.rental.auth.dto.UpdatePasswordRequest;
import com.share.rental.auth.dto.UpdateUserProfileRequest;
import com.share.rental.auth.dto.UserAuditItemResponse;
import com.share.rental.auth.dto.UserAuditRequest;
import com.share.rental.auth.dto.UserAdminStatsResponse;
import com.share.rental.auth.dto.UserMeResponse;
import com.share.rental.auth.dto.UserPublicResponse;
import com.share.rental.auth.entity.CreditScoreRecord;
import com.share.rental.auth.entity.User;
import com.share.rental.auth.mapper.CreditScoreRecordMapper;
import com.share.rental.auth.mapper.UserMapper;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.security.JwtUtil;
import com.share.rental.common.security.SecurityProperties;
import com.share.rental.common.upload.ImageUploadService;
import com.share.rental.common.upload.UploadedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final String AVATAR_BUCKET = "avatars";
    private static final String DEFAULT_PUBLIC_USERNAME = "邻享用户";
    private static final String DEFAULT_PUBLIC_DESCRIPTION = "这个人还没有填写简介";

    private final UserMapper userMapper;
    private final CreditScoreRecordMapper creditScoreRecordMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;
    private final JwtUtil jwtUtil;
    private final ImageUploadService imageUploadService;
    private final RefreshTokenService refreshTokenService;

    @Autowired
    public UserService(UserMapper userMapper,
                           CreditScoreRecordMapper creditScoreRecordMapper,
                           PasswordEncoder passwordEncoder,
                           SecurityProperties securityProperties,
                           JwtUtil jwtUtil,
                           ImageUploadService imageUploadService,
                           RefreshTokenService refreshTokenService) {
        this.userMapper = userMapper;
        this.creditScoreRecordMapper = creditScoreRecordMapper;
        this.passwordEncoder = passwordEncoder;
        this.securityProperties = securityProperties;
        this.jwtUtil = jwtUtil;
        this.imageUploadService = imageUploadService;
        this.refreshTokenService = refreshTokenService;
    }

    public void register(RegisterRequest request) {
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.AUTH_USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreditScore(100);
        user.setRole(0);
        user.setStatus(0);
        user.setUsernameAuditStatus(0);
        user.setAvatarAuditStatus(1);
        user.setDescriptionAuditStatus(1);
        user.setShowRentalHistory(0);
        userMapper.insert(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = findUserByUsername(request.getUsername());
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.AUTH_BAD_CREDENTIALS);
        }
        ensureActive(user);
        return buildLoginResponse(user);
    }

    public LoginResponse adminLogin(LoginRequest request) {
        User user = findUserByUsername(request.getUsername());
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.AUTH_BAD_CREDENTIALS);
        }
        ensureActive(user);
        if (user.getRole() == null || user.getRole() != 1) {
            throw new BusinessException(ErrorCode.AUTH_NOT_ADMIN);
        }
        return buildLoginResponse(user);
    }

    public RefreshResponse refresh(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }
        String token = authorization.substring("Bearer ".length()).trim();
        RefreshTokenService.RefreshTokenPayload refresh = refreshTokenService.rotate(token);
        User user = requireActiveUser(refresh.userId());
        String role = Integer.valueOf(1).equals(user.getRole()) ? "ADMIN" : "USER";
        String token2 = jwtUtil.createToken(user.getId(), user.getUsername(), role, securityProperties.getAccessTokenTtl());
        String nextRefresh = refreshTokenService.issueRefreshToken(user.getId(), user.getUsername(), role);
        long expiresIn = securityProperties.getAccessTokenTtl().get(ChronoUnit.SECONDS);
        return new RefreshResponse(token2, nextRefresh, expiresIn);
    }

    public UserMeResponse getMe(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND);
        }
        return new UserMeResponse(
                user.getId(),
                user.getUsername(),
                user.getAvatarUrl(),
                user.getDescription(),
                user.getCreditScore(),
                user.getRole(),
                user.getStatus(),
                user.getUsernameAuditStatus(),
                user.getAvatarAuditStatus(),
                user.getDescriptionAuditStatus(),
                user.getShowRentalHistory(),
                user.getLastLoginTime()
        );
    }

    public UserPublicResponse getPublic(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND);
        }
        String username = Integer.valueOf(2).equals(user.getUsernameAuditStatus())
                ? DEFAULT_PUBLIC_USERNAME
                : user.getUsername();
        String avatarUrl = Integer.valueOf(2).equals(user.getAvatarAuditStatus())
                ? ""
                : user.getAvatarUrl();
        String description = Integer.valueOf(2).equals(user.getDescriptionAuditStatus())
                ? DEFAULT_PUBLIC_DESCRIPTION
                : user.getDescription();
        return new UserPublicResponse(
                user.getId(),
                username,
                avatarUrl,
                description,
                user.getCreditScore(),
                user.getStatus(),
                user.getShowRentalHistory()
        );
    }

    public User getUserEntity(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND);
        }
        return user;
    }

    public UserMeResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        User user = requireActiveUser(userId);
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, request.getUsername())
                    .ne(User::getId, userId));
            if (existing != null) {
                throw new BusinessException(ErrorCode.AUTH_USERNAME_EXISTS);
            }
            user.setUsername(request.getUsername());
            user.setUsernameAuditStatus(0);
        }
        if (request.getDescription() != null && !request.getDescription().equals(user.getDescription())) {
            user.setDescription(request.getDescription());
            user.setDescriptionAuditStatus(0);
        }
        if (request.getShowRentalHistory() != null) {
            user.setShowRentalHistory(request.getShowRentalHistory());
        }
        userMapper.updateById(user);
        return new UserMeResponse(
                user.getId(),
                user.getUsername(),
                user.getAvatarUrl(),
                user.getDescription(),
                user.getCreditScore(),
                user.getRole(),
                user.getStatus(),
                user.getUsernameAuditStatus(),
                user.getAvatarAuditStatus(),
                user.getDescriptionAuditStatus(),
                user.getShowRentalHistory(),
                user.getLastLoginTime()
        );
    }

    public FileUploadResponse updateAvatar(Long userId, MultipartFile file) {
        User user = requireActiveUser(userId);
        UploadedFile uploaded = imageUploadService.storeImage(file, AVATAR_BUCKET);
        user.setAvatarUrl(uploaded.url());
        user.setAvatarAuditStatus(0);
        userMapper.updateById(user);
        return new FileUploadResponse(
                uploaded.url(),
                uploaded.filename(),
                uploaded.contentType(),
                uploaded.size()
        );
    }

    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        User user = requireActiveUser(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.AUTH_BAD_CREDENTIALS);
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    public UserAdminStatsResponse getAdminStats() {
        long totalUsers = userMapper.selectCount(null);
        long bannedUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1));
        return new UserAdminStatsResponse(totalUsers, bannedUsers);
    }

    public List<UserAuditItemResponse> listUserAudits(Integer auditStatus) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>();
        if (auditStatus == null) {
            // "全部"筛选：返回任一审核字段处于待审核(0)或要求整改(2)的用户，
            // 已全部通过的(1,1,1)不出现在审核列表中。这样 dashboard 统计与
            // 前端"全部"tab 都能看到待处理审核数据。
            wrapper.and(w -> w
                    .eq(User::getUsernameAuditStatus, 0).or()
                    .eq(User::getAvatarAuditStatus, 0).or()
                    .eq(User::getDescriptionAuditStatus, 0).or()
                    .eq(User::getUsernameAuditStatus, 2).or()
                    .eq(User::getAvatarAuditStatus, 2).or()
                    .eq(User::getDescriptionAuditStatus, 2));
        } else {
            wrapper.and(w -> w
                    .eq(User::getUsernameAuditStatus, auditStatus).or()
                    .eq(User::getAvatarAuditStatus, auditStatus).or()
                    .eq(User::getDescriptionAuditStatus, auditStatus));
        }
        List<User> users = userMapper.selectList(wrapper);
        List<UserAuditItemResponse> items = new ArrayList<>();
        for (User user : users) {
            UserAuditItemResponse item = buildUserAuditItem(user, auditStatus);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private UserAuditItemResponse buildUserAuditItem(User user, Integer auditStatus) {
        List<String> fieldNames = collectMatchingFields(user, auditStatus);
        if (fieldNames.isEmpty()) {
            return null;
        }
        String primaryField = fieldNames.get(0);
        Integer primaryStatus = fieldStatusOf(user, primaryField);
        List<Integer> auditStatuses = fieldNames.stream()
                .map(f -> fieldStatusOf(user, f))
                .collect(Collectors.toList());
        return new UserAuditItemResponse(
                user.getId(),
                user.getUsername(),
                primaryField,
                primaryStatus,
                null,
                user.getAvatarUrl(),
                user.getDescription(),
                user.getCreditScore(),
                user.getStatus(),
                user.getShowRentalHistory(),
                user.getUsernameAuditStatus(),
                user.getAvatarAuditStatus(),
                user.getDescriptionAuditStatus(),
                fieldNames,
                auditStatuses
        );
    }

    private List<String> collectMatchingFields(User user, Integer auditStatus) {
        List<String> fields = new ArrayList<>();
        if (fieldMatches(user.getUsernameAuditStatus(), true, auditStatus)) {
            fields.add("username");
        }
        if (fieldMatches(user.getAvatarAuditStatus(), hasText(user.getAvatarUrl()), auditStatus)) {
            fields.add("avatar");
        }
        if (fieldMatches(user.getDescriptionAuditStatus(), hasText(user.getDescription()), auditStatus)) {
            fields.add("description");
        }
        return fields;
    }

    private Integer fieldStatusOf(User user, String fieldName) {
        return switch (fieldName) {
            case "username" -> user.getUsernameAuditStatus();
            case "avatar" -> user.getAvatarAuditStatus();
            case "description" -> user.getDescriptionAuditStatus();
            default -> null;
        };
    }

    private boolean fieldMatches(Integer fieldStatus, boolean hasSubmittedContent, Integer auditStatus) {
        if (fieldStatus == null) {
            return false;
        }
        if (!hasSubmittedContent) {
            return false;
        }
        if (auditStatus != null) {
            return Objects.equals(auditStatus, fieldStatus);
        }
        return fieldStatus == 0 || fieldStatus == 2;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Note: audit reason and admin identity are persisted by admin-service in audit_records.
     * auth-service only performs the field audit_status state change.
     */
    public void auditUser(Long adminId, Long userId, UserAuditRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND);
        }
        Integer auditStatus = request.getAuditStatus();
        if (auditStatus == null || (auditStatus != 1 && auditStatus != 2)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "审核状态非法");
        }
        if (auditStatus == 2 && (request.getReason() == null || request.getReason().isBlank())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "整改必须填写原因");
        }
        switch (request.getFieldName()) {
            case "username" -> user.setUsernameAuditStatus(auditStatus);
            case "avatar" -> user.setAvatarAuditStatus(auditStatus);
            case "description" -> user.setDescriptionAuditStatus(auditStatus);
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "审核字段名非法");
        }
        userMapper.updateById(user);
    }

    /**
     * Note: ban reason and admin identity are persisted by admin-service in admin_operation_logs.
     * auth-service only performs the user status state change.
     */
    public void banUser(Long adminId, Long userId, BanUserRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND);
        }
        if (Integer.valueOf(1).equals(user.getRole())) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_ALLOWED, "不能封禁管理员账户");
        }
        user.setStatus(1);
        userMapper.updateById(user);
    }

    /**
     * Note: unban admin identity is persisted by admin-service in admin_operation_logs.
     * auth-service only performs the user status state change.
     */
    public void unbanUser(Long adminId, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND);
        }
        user.setStatus(0);
        userMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void adjustCreditScore(Long userId, CreditScoreAdjustRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND);
        }
        int before = user.getCreditScore() == null ? 100 : user.getCreditScore();
        int change = request.getChangeValue() == null ? 0 : request.getChangeValue();
        int after = Math.max(0, before + change);
        user.setCreditScore(after);
        userMapper.updateById(user);

        CreditScoreRecord record = new CreditScoreRecord();
        record.setUserId(userId);
        record.setOrderId(request.getOrderId());
        record.setChangeValue(change);
        record.setBeforeScore(before);
        record.setAfterScore(after);
        record.setReasonType(request.getReasonType());
        record.setReason(request.getReason());
        creditScoreRecordMapper.insert(record);
    }

    private User requireActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND);
        }
        ensureActive(user);
        return user;
    }

    private void ensureActive(User user) {
        if (Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_BANNED);
        }
    }

    private User findUserByUsername(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_BAD_CREDENTIALS);
        }
        return user;
    }

    private LoginResponse buildLoginResponse(User user) {
        String role = (user.getRole() != null && user.getRole() == 1) ? "ADMIN" : "USER";
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), role, securityProperties.getAccessTokenTtl());
        String refreshToken = refreshTokenService.issueRefreshToken(user.getId(), user.getUsername(), role);
        long expiresIn = securityProperties.getAccessTokenTtl().get(ChronoUnit.SECONDS);
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        return new LoginResponse(user.getId(), user.getUsername(), role, token, refreshToken, expiresIn);
    }
}
