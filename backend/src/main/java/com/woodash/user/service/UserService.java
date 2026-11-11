package com.woodash.user.service;

import java.util.Optional;
import java.util.UUID;

import com.woodash.auth.repository.RefreshTokenRepository;
import com.woodash.common.ErrorCode;
import com.woodash.common.PageRequest;
import com.woodash.common.PageResponse;
import com.woodash.user.entity.User;
import com.woodash.user.repository.UserRepository;
import com.woodash.user.rest.dto.ChangePasswordRequest;
import com.woodash.user.rest.dto.ResetPasswordRequest;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    @Inject
    RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public boolean loginExist(String login) {
        Optional<User> user = userRepository.findByLogin(login);
        return user.isPresent();
    }

    public User createUser(String login, String passwordHash) {
        return userRepository.createUser(login, passwordHash);
    }

    @Transactional
    public User getUserByLogin(String login) {
        return userRepository.findByLogin(login).orElse(null);
    }

    public Optional<User> getUserById(UUID id) {
        return userRepository.findByIdOptional(id);
    }

    public PageResponse<User> listPage(String search, PageRequest page) {
        return userRepository.searchPage(search, page);
    }

    @Transactional
    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        if (request == null || request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()
                || !validPassword(request.getNewPassword())) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }

        User user = userRepository.findByIdOptional(userId).orElseThrow(ErrorCode.NOT_FOUND::getException);
        if (!BcryptUtil.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            ErrorCode.INVALID_CURRENT_PASSWORD.throwException();
        }
        if (BcryptUtil.matches(request.getNewPassword(), user.getPasswordHash())) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }

        user.setPasswordHash(BcryptUtil.bcryptHash(request.getNewPassword()));
        userRepository.flush();
        // A password change invalidates every existing refresh token, including tokens
        // issued to other devices before the change.
        refreshTokenRepository.revokeAllForUser(userId);
    }

    @Transactional
    public void resetPassword(UUID userId, ResetPasswordRequest request) {
        if (request == null || !validPassword(request.getNewPassword())) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }

        User user = userRepository.findByIdOptional(userId).orElseThrow(ErrorCode.NOT_FOUND::getException);
        user.setPasswordHash(BcryptUtil.bcryptHash(request.getNewPassword()));
        userRepository.flush();
        // A password reset invalidates every existing session of that account.
        refreshTokenRepository.revokeAllForUser(userId);
    }

    private boolean validPassword(String password) {
        return password != null && password.length() >= 8 && password.length() <= 128;
    }
}
