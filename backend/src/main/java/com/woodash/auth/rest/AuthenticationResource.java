package com.woodash.auth.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.woodash.auth.rest.dto.LoginRequest;
import com.woodash.auth.rest.dto.RegisterRequest;
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
    AuthenticationService authenticationResource;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/register")
    public Response resgiterUser(@JsonView(RegisterRequest.class) RegisterRequest register) {
        return Response.ok(authenticationResource.resgisterUser(register)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/login")
    public Response loginUser(@JsonView(LoginRequest.class) LoginRequest login) {
        return Response.ok().build();
    }
}
