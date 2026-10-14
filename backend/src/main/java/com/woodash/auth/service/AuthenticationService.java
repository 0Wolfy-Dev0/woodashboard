package com.woodash.auth.service;

import com.woodash.auth.rest.dto.RegisterRequest;
import com.woodash.common.ErrorCode;
import com.woodash.user.entity.User;
import com.woodash.user.rest.dto.UserResponse;
import com.woodash.user.service.UserService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuthenticationService {

    @Inject
    UserService userService;

    public UserResponse resgisterUser(RegisterRequest register) {
        if (register == null || register.getLogin() != null || register.getLogin().isEmpty()
                || register.getPassword() != null || register.getPassword().isEmpty()) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }

        if (userService.loginExist(register.getLogin())) {
            ErrorCode.LOGIN_ALREADY_TAKEN.throwException();
        }

        userService.createUser(register.getLogin(), register.getPassword());
        User user = userService.getUserByLogin(register.getLogin());
        return new UserResponse(user.getId(), user.getLogin(), user.getRole(), user.getCreationTime(),
                user.getLastModificationTime());
    }
}
