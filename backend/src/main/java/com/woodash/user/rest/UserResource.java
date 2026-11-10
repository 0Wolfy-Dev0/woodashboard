package com.woodash.user.rest;

import com.woodash.auth.security.CurrentUser;
import com.woodash.common.ErrorCode;
import com.woodash.user.rest.dto.ChangePasswordRequest;
import com.woodash.user.rest.dto.UserResponse;
import com.woodash.user.service.UserService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/me")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    CurrentUser currentUser;

    @Inject
    UserService userService;

    @GET
    public UserResponse getMe() {
        return userService.getUserById(currentUser.getUserId())
                .map(user -> new UserResponse(user.getId(), user.getLogin(), user.getRole(), user.getCreationTime(),
                        user.getLastModificationTime()))
                .orElseThrow(ErrorCode.NOT_FOUND::getException);
    }

    @PATCH
    @Path("/password")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changePassword(ChangePasswordRequest request) {
        userService.changePassword(currentUser.getUserId(), request);
        return Response.noContent().build();
    }
}
