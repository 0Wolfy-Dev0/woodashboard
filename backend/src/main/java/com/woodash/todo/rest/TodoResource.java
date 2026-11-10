package com.woodash.todo.rest;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.woodash.auth.security.CurrentUser;
import com.woodash.todo.rest.dto.TodoCreateRequest;
import com.woodash.todo.rest.dto.TodoResponse;
import com.woodash.todo.rest.dto.TodoUpdateRequest;
import com.woodash.todo.service.TodoService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/todos")
@Produces(MediaType.APPLICATION_JSON)
public class TodoResource {

    @Inject
    TodoService todoService;

    @Inject
    CurrentUser currentUser;

    @GET
    public List<TodoResponse> list(@QueryParam("completed") Boolean completed,
            @QueryParam("groupId") UUID groupId, @QueryParam("folderId") UUID folderId) {
        return todoService.listAccessible(currentUser.getUserId(), completed, groupId, folderId, currentUser);
    }

    @GET
    @Path("/{userId}")
    public List<TodoResponse> listByUser(@PathParam("userId") UUID userId,
            @QueryParam("completed") Boolean completed) {
        return todoService.listByUser(userId, completed, currentUser);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(TodoCreateRequest request) {
        TodoResponse todo = todoService.create(request, currentUser);
        return Response.created(URI.create("/api/todos/" + todo.getId())).entity(todo).build();
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public TodoResponse update(@PathParam("id") UUID id, TodoUpdateRequest request) {
        return todoService.update(id, request, currentUser);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        todoService.delete(id, currentUser);
        return Response.noContent().build();
    }
}
