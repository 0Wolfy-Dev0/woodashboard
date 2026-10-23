package com.woodash.group.rest;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.woodash.auth.security.CurrentUser;
import com.woodash.group.rest.dto.GroupCreateRequest;
import com.woodash.group.rest.dto.GroupMemberRequest;
import com.woodash.group.rest.dto.GroupResponse;
import com.woodash.group.rest.dto.GroupUpdateRequest;
import com.woodash.group.service.GroupService;

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

@Path("/api/groups")
@Produces(MediaType.APPLICATION_JSON)
public class GroupResource {

    @Inject
    GroupService groupService;

    @Inject
    CurrentUser currentUser;

    @GET
    public List<GroupResponse> list() {
        return groupService.listForUser(currentUser);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(GroupCreateRequest request) {
        GroupResponse group = groupService.create(request, currentUser.getUserId());
        return Response.created(URI.create("/api/groups/" + group.getId())).entity(group).build();
    }

    @GET
    @Path("/{id}")
    public GroupResponse get(@PathParam("id") UUID id) {
        return groupService.get(id, currentUser);
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public GroupResponse update(@PathParam("id") UUID id, GroupUpdateRequest request) {
        return groupService.update(id, request, currentUser);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        groupService.delete(id, currentUser);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/members")
    @Consumes(MediaType.APPLICATION_JSON)
    public GroupResponse addMember(@PathParam("id") UUID id, GroupMemberRequest request) {
        return groupService.addMember(id, request, currentUser);
    }

    @DELETE
    @Path("/{id}/members/{userId}")
    public Response removeMember(@PathParam("id") UUID id, @PathParam("userId") UUID userId) {
        groupService.removeMember(id, userId, currentUser);
        return Response.noContent().build();
    }
}
