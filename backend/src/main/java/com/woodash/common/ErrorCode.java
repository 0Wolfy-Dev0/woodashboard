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
    LOGIN_ALREADY_TAKEN(Status.CONFLICT, "Login is already taken");

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
