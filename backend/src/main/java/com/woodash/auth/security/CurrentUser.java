package com.woodash.auth.security;

import java.util.UUID;

import com.woodash.common.ErrorCode;
import com.woodash.user.entity.Role;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class CurrentUser {
    private UUID userId;
    private Role role;

    public void set(AccessTokenClaims claims) {
        this.userId = claims.userId();
        this.role = claims.role();
    }

    public UUID getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            ErrorCode.FORBIDDEN.throwException();
        }
    }

    public void requireOwnerOrAdmin(UUID ownerId) {
        if (!isAdmin() && !userId.equals(ownerId)) {
            ErrorCode.FORBIDDEN.throwException();
        }
    }
}
