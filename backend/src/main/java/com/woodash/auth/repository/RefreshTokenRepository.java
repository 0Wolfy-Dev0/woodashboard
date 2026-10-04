package com.woodash.auth.repository;

import com.woodash.auth.entity.RefreshToken;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RefreshTokenRepository implements PanacheRepositoryBase<RefreshToken, UUID> {

    public Optional<RefreshToken> findByHash(String hash) {
        return find("tokenHash", hash).firstResultOptional();
    }

    public long revokeAllForUser(UUID userId) {
        return update("revoked = true where user.id = ?1 and revoked = false", userId);
    }

    public long deleteExpired(LocalDateTime now) {
        return delete("expirationTime < ?1", now);
    }
}
