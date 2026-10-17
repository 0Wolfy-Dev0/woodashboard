package com.woodash.auth.service;

import com.woodash.auth.rest.dto.RegisterRequest;
import com.woodash.auth.rest.dto.LoginRequest;
import com.woodash.auth.rest.dto.RefreshRequest;
import com.woodash.auth.rest.dto.TokenResponse;
import com.woodash.auth.entity.RefreshToken;
import com.woodash.auth.repository.RefreshTokenRepository;
import com.woodash.common.ErrorCode;
import com.woodash.user.entity.User;
import com.woodash.user.rest.dto.UserResponse;
import com.woodash.user.service.UserService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@ApplicationScoped
public class AuthenticationService {

    @Inject
    UserService userService;

    @Inject
    RefreshTokenRepository refreshTokenRepository;

    @Inject
    TokenService tokenService;

    @Transactional
    public UserResponse registerUser(RegisterRequest register) {
        if (register == null || !validLogin(register.getLogin()) || !validPassword(register.getPassword())) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }

        if (userService.loginExist(register.getLogin())) {
            ErrorCode.LOGIN_ALREADY_TAKEN.throwException();
        }

        return toUserResponse(userService.createUser(register.getLogin(), register.getPassword()));
    }

    @Transactional
    public TokenResponse login(LoginRequest login) {
        if (login == null || login.getLogin() == null || login.getLogin().isBlank()
                || login.getPassword() == null || login.getPassword().isEmpty()) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }

        User user = userService.getUserByLogin(login.getLogin());
        if (user == null || !io.quarkus.elytron.security.common.BcryptUtil.matches(login.getPassword(),
                user.getPasswordHash())) {
            ErrorCode.INVALID_CREDENTIALS.throwException();
        }
        return tokenService.createTokenPair(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }

        Optional<RefreshToken> storedToken = refreshTokenRepository
                .findByHashForUpdate(TokenService.hashRefreshToken(request.getRefreshToken()));
        if (storedToken.isEmpty()) {
            ErrorCode.INVALID_REFRESH_TOKEN.throwException();
        }

        RefreshToken oldToken = storedToken.get();
        if (oldToken.isRevoked() || !oldToken.getExpirationTime().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            ErrorCode.INVALID_REFRESH_TOKEN.throwException();
        }

        oldToken.setRevoked(true);
        return tokenService.createTokenPair(oldToken.getUser());
    }

    @Transactional
    public void logout(RefreshRequest request, java.util.UUID currentUserId) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }

        refreshTokenRepository.findByHash(TokenService.hashRefreshToken(request.getRefreshToken()))
                .filter(token -> token.getUser().getId().equals(currentUserId))
                .ifPresent(token -> token.setRevoked(true));
    }

    private boolean validLogin(String login) {
        return login != null && !login.isBlank() && login.length() >= 3 && login.length() <= 64;
    }

    private boolean validPassword(String password) {
        return password != null && password.length() >= 8 && password.length() <= 128;
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getLogin(), user.getRole(), user.getCreationTime(),
                user.getLastModificationTime());
    }
}
