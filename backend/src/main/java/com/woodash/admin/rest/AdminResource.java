package com.woodash.admin.rest;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import com.woodash.auth.security.CurrentUser;
import com.woodash.common.ErrorCode;
import com.woodash.common.PageRequest;
import com.woodash.common.PageResponse;
import com.woodash.event.rest.dto.EventResponse;
import com.woodash.event.service.EventService;
import com.woodash.todo.rest.dto.TodoResponse;
import com.woodash.todo.service.TodoService;
import com.woodash.user.rest.dto.ResetPasswordRequest;
import com.woodash.user.rest.dto.UserResponse;
import com.woodash.user.service.UserService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/admin")
@Produces(MediaType.APPLICATION_JSON)
public class AdminResource {

    @Inject
    CurrentUser currentUser;

    @Inject
    UserService userService;

    @Inject
    EventService eventService;

    @Inject
    TodoService todoService;

    @GET
    @Path("/users")
    public PageResponse<UserResponse> listUsers(@QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("25") int size, @QueryParam("search") String search) {
        currentUser.requireAdmin();
        PageRequest request = PageRequest.of(page, size);
        return userService.listPage(normalizeSearch(search), request)
                .map(user -> new UserResponse(user.getId(), user.getLogin(), user.getRole(), user.getCreationTime(),
                        user.getLastModificationTime()));
    }

    @DELETE
    @Path("/users/{id}")
    public Response deleteUser(@PathParam("id") UUID id) {
        currentUser.requireAdmin();
        if (userService.getUserById(id).isEmpty()) {
            ErrorCode.NOT_FOUND.throwException();
        }
        userService.deleteUser(id);
        return Response.noContent().build();
    }

    @PATCH
    @Path("/users/{id}/password")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response resetPassword(@PathParam("id") UUID id, ResetPasswordRequest request) {
        currentUser.requireAdmin();
        userService.resetPassword(id, request);
        return Response.noContent().build();
    }

    @GET
    @Path("/events")
    public PageResponse<EventResponse> listEvents(@QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("25") int size, @QueryParam("search") String search,
            @QueryParam("from") String fromValue, @QueryParam("to") String toValue) {
        currentUser.requireAdmin();
        return eventService.listAllPage(normalizeSearch(search), parseDateTime(fromValue), parseDateTime(toValue),
                PageRequest.of(page, size));
    }

    @GET
    @Path("/todos")
    public PageResponse<TodoResponse> listTodos(@QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("25") int size, @QueryParam("search") String search) {
        currentUser.requireAdmin();
        return todoService.listAllPage(normalizeSearch(search), PageRequest.of(page, size));
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String trimmed = search.trim();
        if (trimmed.length() > 100) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
            return null;
        }
    }
}
