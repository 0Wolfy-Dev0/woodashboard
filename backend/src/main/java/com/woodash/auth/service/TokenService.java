package com.woodash.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woodash.auth.entity.RefreshToken;
import com.woodash.auth.repository.RefreshTokenRepository;
import com.woodash.auth.rest.dto.TokenResponse;
import com.woodash.auth.security.AccessTokenClaims;
import com.woodash.user.entity.Role;
import com.woodash.user.entity.User;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TokenService {
    private static final String JWT_HEADER = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long REFRESH_TOKEN_LIFETIME_DAYS = 30;

    @Inject
    RefreshTokenRepository refreshTokenRepository;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "app.jwt.secret")
    String jwtSecret;

    @ConfigProperty(name = "app.jwt.access-token-seconds", defaultValue = "900")
    long accessTokenLifetimeSeconds;

    public TokenResponse createTokenPair(User user) {
        String refreshTokenValue = createOpaqueRefreshToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashRefreshToken(refreshTokenValue));
        refreshToken.setExpirationTime(LocalDateTime.now(ZoneOffset.UTC).plusDays(REFRESH_TOKEN_LIFETIME_DAYS));
        refreshTokenRepository.persist(refreshToken);

        return new TokenResponse(createAccessToken(user), refreshTokenValue, "Bearer", accessTokenLifetimeSeconds);
    }

    public AccessTokenClaims verifyAccessToken(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3 || !JWT_HEADER.equals(parts[0])) {
                throw new IllegalArgumentException("Invalid token");
            }

            byte[] expectedSignature = sign(parts[0] + "." + parts[1]);
            byte[] actualSignature = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                throw new IllegalArgumentException("Invalid signature");
            }

            JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            String subject = claims.path("sub").asText(null);
            String roleName = claims.path("role").asText(null);
            long expiresAt = claims.path("exp").asLong(0);
            if (subject == null || roleName == null || expiresAt <= Instant.now().getEpochSecond()) {
                throw new IllegalArgumentException("Expired or incomplete token");
            }

            return new AccessTokenClaims(UUID.fromString(subject), Role.valueOf(roleName));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid access token", exception);
        }
    }

    public static String hashRefreshToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String createAccessToken(User user) {
        long now = Instant.now().getEpochSecond();
        long expiresAt = now + accessTokenLifetimeSeconds;
        String payload = "{\"sub\":\"" + user.getId() + "\",\"role\":\"" + user.getRole()
                + "\",\"iat\":" + now + ",\"exp\":" + expiresAt + "}";
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signedContent = JWT_HEADER + "." + encodedPayload;
        return signedContent + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(signedContent));
    }

    private String createOpaqueRefreshToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign access token", exception);
        }
    }
}
