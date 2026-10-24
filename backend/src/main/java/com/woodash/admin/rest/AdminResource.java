package com.woodash.admin.rest;

import java.util.List;
import java.util.UUID;

import com.woodash.auth.security.CurrentUser;
import com.woodash.common.ErrorCode;
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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
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
    public List<UserResponse> listUsers() {
        currentUser.requireAdmin();
        return userService.listAll().stream()
                .map(user -> new UserResponse(user.getId(), user.getLogin(), user.getRole(), user.getCreationTime(),
                        user.getLastModificationTime()))
                .toList();
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
    public List<EventResponse> listEvents() {
        currentUser.requireAdmin();
        return eventService.listAll();
    }

    @GET
    @Path("/todos")
    public List<TodoResponse> listTodos() {
        currentUser.requireAdmin();
        return todoService.listAll();
    }
}
