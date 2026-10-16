package com.woodash.auth.security;

import java.util.UUID;

import com.woodash.user.entity.Role;

public record AccessTokenClaims(UUID userId, Role role) {
}
