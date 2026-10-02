package com.share.rental.common.security;

public record JwtClaims(Long userId, String username, String role) {
}
