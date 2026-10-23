package com.woodash.common;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static jakarta.ws.rs.core.Response.Status;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_REQUEST_BODY(Status.BAD_REQUEST, "Invalid argument of the request body"),
    LOGIN_ALREADY_TAKEN(Status.CONFLICT, "Login is already taken"),
    INVALID_CREDENTIALS(Status.UNAUTHORIZED, "Invalid login or password"),
    INVALID_REFRESH_TOKEN(Status.UNAUTHORIZED, "Refresh token is invalid, expired or revoked"),
    UNAUTHORIZED(Status.UNAUTHORIZED, "Missing, invalid or expired access token"),
    FORBIDDEN(Status.FORBIDDEN, "You are not allowed to access this resource"),
    NOT_FOUND(Status.NOT_FOUND, "Resource not found"),
    GROUP_MEMBER_ALREADY_EXISTS(Status.CONFLICT, "User is already a member of this group"),
    CANNOT_REMOVE_GROUP_OWNER(Status.BAD_REQUEST, "The group owner cannot be removed");

    private final Response.Status errorCode;

    private final String errorMessage;

    public WebApplicationException getException() {
        return new WebApplicationException(Response.status(errorCode).entity(new ErrorInfo(errorMessage)).build());
    }

    public void throwException() {
        throw getException();
    }

    public void throwException(String prefix) {
        throw new WebApplicationException(
                Response.status(errorCode).entity(new ErrorInfo(prefix + ": " + errorMessage)).build());
    }
}
