package fr.epita.assistants.yakamon.utils;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static jakarta.ws.rs.core.Response.Status;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    EXAMPLE_ERROR(Status.INTERNAL_SERVER_ERROR, "This is an error example"),
    INTERNAL_ERROR(Status.INTERNAL_SERVER_ERROR, "An internal server error occurred"),
    PLAYER_NOT_FOUND(Status.NOT_FOUND, "The player was not found in the database"),
    INVALID_START_BODY(Status.BAD_REQUEST, "Invalid body argument for start endpoint"),
    GAME_NOT_START(Status.BAD_REQUEST, "Game didn't start yet"),
    RECENTLY_MOVED(Status.TOO_MANY_REQUESTS, "The player already move recently"),
    RECENTLY_CATCHED(Status.TOO_MANY_REQUESTS, "The player already catch recently"),
    RECENTLY_COLLECT(Status.TOO_MANY_REQUESTS, "The player already collect recently"),
    RECENTLY_FEED(Status.TOO_MANY_REQUESTS, "The player already feed recently"),
    INVALID_TILE(Status.BAD_REQUEST, "Invalid tile"),
    BAD_INVENTORY(Status.BAD_REQUEST, "Inventory incomplete"),
    TOO_MANY_YAKAMON(Status.BAD_REQUEST, "Too many yakamon"),
    YAKAMON_NOT_EXIST(Status.NOT_FOUND, "Yakamon does not exist"),
    CANNOT_RELEASE_YAKAMON(Status.FORBIDDEN, "Cannot release yakamon here");

    
    private final Response.Status errorCode;

    private final String errorMessage;

    public WebApplicationException getException() {
        return new WebApplicationException(Response.status(errorCode).entity(new ErrorInfo(errorMessage)).build());
    }

    public void throwException() {
        throw getException();
    }

    public void throwException(String prefix) {
        throw new WebApplicationException(Response.status(errorCode).entity(new ErrorInfo(prefix + ": " + errorMessage)).build());
    }
}
