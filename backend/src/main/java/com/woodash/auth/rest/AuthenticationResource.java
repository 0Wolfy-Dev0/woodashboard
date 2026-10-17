package com.woodash.auth.rest;

import com.woodash.auth.rest.dto.LoginRequest;
import com.woodash.auth.rest.dto.RefreshRequest;
import com.woodash.auth.rest.dto.RegisterRequest;
import com.woodash.auth.security.CurrentUser;
import com.woodash.auth.service.AuthenticationService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/auth")
public class AuthenticationResource {

    @Inject
    AuthenticationService authenticationService;

    @Inject
    CurrentUser currentUser;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/register")
    public Response registerUser(RegisterRequest register) {
        return Response.status(Response.Status.CREATED).entity(authenticationService.registerUser(register)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/login")
    public Response loginUser(LoginRequest login) {
        return Response.ok(authenticationService.login(login)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/refresh")
    public Response refresh(RefreshRequest request) {
        return Response.ok(authenticationService.refresh(request)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/logout")
    public Response logout(RefreshRequest request) {
        authenticationService.logout(request, currentUser.getUserId());
        return Response.noContent().build();
    }
}
