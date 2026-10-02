package com.share.rental.auth.service;

import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.redis.RedisKey;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final String TOKEN_PREFIX = "rt_user_";
    private static final Duration REFRESH_TTL = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String issueRefreshToken(Long userId, String username, String role) {
        String tokenId = UUID.randomUUID().toString().replace("-", "");
        String token = TOKEN_PREFIX + userId + "_" + tokenId;
        redisTemplate.opsForValue().set(key(userId, tokenId), username + "|" + role, REFRESH_TTL);
        return token;
    }

    public RefreshTokenPayload rotate(String token) {
        ParsedToken parsed = parse(token);
        String key = key(parsed.userId(), parsed.tokenId());
        String value = redisTemplate.opsForValue().getAndDelete(key);
        if (value == null || !value.contains("|")) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }
        String[] parts = value.split("\\|", 2);
        // Only consume here. The caller checks the latest user state before issuing a successor.
        return new RefreshTokenPayload(parsed.userId(), parts[0], parts[1]);
    }

    private ParsedToken parse(String token) {
        if (token == null || !token.startsWith(TOKEN_PREFIX)) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }
        String payload = token.substring(TOKEN_PREFIX.length());
        int split = payload.indexOf('_');
        if (split <= 0 || split == payload.length() - 1) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }
        try {
            Long userId = Long.valueOf(payload.substring(0, split));
            String tokenId = payload.substring(split + 1);
            return new ParsedToken(userId, tokenId);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }
    }

    private String key(Long userId, String tokenId) {
        return RedisKey.REFRESH_TOKEN + userId + ":" + tokenId;
    }

    private record ParsedToken(Long userId, String tokenId) {
    }

    public record RefreshTokenPayload(Long userId, String username, String role) {
    }
}
