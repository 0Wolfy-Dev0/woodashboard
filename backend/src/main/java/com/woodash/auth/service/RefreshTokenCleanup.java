package com.woodash.auth.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.woodash.auth.repository.RefreshTokenRepository;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RefreshTokenCleanup {

    @Inject
    RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    void deleteExpiredTokens() {
        refreshTokenRepository.deleteExpired(LocalDateTime.now(ZoneOffset.UTC));
    }
}
