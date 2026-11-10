package com.woodash.todo.rest;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.woodash.auth.security.CurrentUser;
import com.woodash.todo.rest.dto.TodoFolderCreateRequest;
import com.woodash.todo.rest.dto.TodoFolderResponse;
import com.woodash.todo.rest.dto.TodoFolderUpdateRequest;
import com.woodash.todo.service.TodoFolderService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/todo-folders")
@Produces(MediaType.APPLICATION_JSON)
public class TodoFolderResource {

    @Inject
    TodoFolderService todoFolderService;

    @Inject
    CurrentUser currentUser;

    @GET
    public List<TodoFolderResponse> list() {
        return todoFolderService.list(currentUser);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(TodoFolderCreateRequest request) {
        TodoFolderResponse folder = todoFolderService.create(request, currentUser);
        return Response.created(URI.create("/api/todo-folders/" + folder.getId())).entity(folder).build();
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public TodoFolderResponse update(@PathParam("id") UUID id, TodoFolderUpdateRequest request) {
        return todoFolderService.update(id, request, currentUser);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        todoFolderService.delete(id, currentUser);
        return Response.noContent().build();
    }
}
