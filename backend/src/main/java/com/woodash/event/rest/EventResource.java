package com.woodash.event.rest;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import com.woodash.auth.security.CurrentUser;
import com.woodash.common.ErrorCode;
import com.woodash.event.rest.dto.EventCreateRequest;
import com.woodash.event.rest.dto.EventResponse;
import com.woodash.event.rest.dto.EventUpdateRequest;
import com.woodash.event.service.EventService;

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

@Path("/api/events")
@Produces(MediaType.APPLICATION_JSON)
public class EventResource {

    @Inject
    EventService eventService;

    @Inject
    CurrentUser currentUser;

    @GET
    public List<EventResponse> list(@QueryParam("from") String fromValue, @QueryParam("to") String toValue) {
        return eventService.listByUser(currentUser.getUserId(), parseDateTime(fromValue), parseDateTime(toValue));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(EventCreateRequest request) {
        EventResponse event = eventService.create(request, currentUser.getUserId());
        return Response.created(URI.create("/api/events/" + event.getId())).entity(event).build();
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public EventResponse update(@PathParam("id") UUID id, EventUpdateRequest request) {
        return eventService.update(id, request, currentUser);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        eventService.delete(id, currentUser);
        return Response.noContent().build();
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
