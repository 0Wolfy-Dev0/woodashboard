package com.woodash.auth.security;

import com.woodash.auth.service.TokenService;
import com.woodash.common.ErrorInfo;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AccessTokenFilter implements ContainerRequestFilter {

    @Inject
    TokenService tokenService;

    @Inject
    CurrentUser currentUser;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getRequestUri().getPath();
        if (!path.startsWith("/api/")) {
            return;
        }

        if (path.equals("/api/auth/register") || path.equals("/api/auth/login")
                || path.equals("/api/auth/refresh")) {
            return;
        }

        String authorization = requestContext.getHeaderString("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            abortUnauthorized(requestContext);
            return;
        }

        try {
            currentUser.set(tokenService.verifyAccessToken(authorization.substring("Bearer ".length())));
        } catch (RuntimeException exception) {
            abortUnauthorized(requestContext);
        }
    }

    private void abortUnauthorized(ContainerRequestContext requestContext) {
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorInfo("Missing, invalid or expired access token"))
                .build());
    }
}
