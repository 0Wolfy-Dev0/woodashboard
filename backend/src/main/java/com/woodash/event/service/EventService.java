package com.woodash.event.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.woodash.auth.security.CurrentUser;
import com.woodash.common.ErrorCode;
import com.woodash.event.entity.Event;
import com.woodash.event.repository.EventRepository;
import com.woodash.event.rest.dto.EventCreateRequest;
import com.woodash.event.rest.dto.EventResponse;
import com.woodash.event.rest.dto.EventUpdateRequest;
import com.woodash.user.repository.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class EventService {

    @Inject
    EventRepository eventRepository;

    @Inject
    UserRepository userRepository;

    public List<EventResponse> listByUser(UUID userId, LocalDateTime from, LocalDateTime to) {
        validateWindow(from, to);
        List<Event> events;
        if (from != null && to != null) {
            events = eventRepository.listByUserBetween(userId, from, to);
        } else if (from != null) {
            events = eventRepository.listByUserFrom(userId, from);
        } else if (to != null) {
            events = eventRepository.listByUserTo(userId, to);
        } else {
            events = eventRepository.listByUser(userId);
        }
        return events.stream().map(EventResponse::from).toList();
    }

    @Transactional
    public EventResponse create(EventCreateRequest request, UUID userId) {
        if (request == null) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }
        validateName(request.getName());
        validateTimes(request.getStartTime(), request.getEndTime());
        var user = userRepository.findByIdOptional(userId).orElseThrow(ErrorCode.NOT_FOUND::getException);

        Event event = new Event();
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setUser(user);
        eventRepository.persistAndFlush(event);
        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse update(UUID id, EventUpdateRequest request, CurrentUser currentUser) {
        if (request == null || !request.hasUpdates()) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }
        Event event = eventRepository.findByIdOptional(id).orElseThrow(ErrorCode.NOT_FOUND::getException);
        currentUser.requireOwnerOrAdmin(event.getUser().getId());

        String name = request.hasName() ? request.getName() : event.getName();
        LocalDateTime startTime = request.hasStartTime() ? request.getStartTime() : event.getStartTime();
        LocalDateTime endTime = request.hasEndTime() ? request.getEndTime() : event.getEndTime();
        validateName(name);
        validateTimes(startTime, endTime);

        event.setName(name);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        if (request.hasDescription()) {
            event.setDescription(request.getDescription());
        }
        eventRepository.flush();
        return EventResponse.from(event);
    }

    @Transactional
    public void delete(UUID id, CurrentUser currentUser) {
        Event event = eventRepository.findByIdOptional(id).orElseThrow(ErrorCode.NOT_FOUND::getException);
        currentUser.requireOwnerOrAdmin(event.getUser().getId());
        eventRepository.delete(event);
    }

    public List<EventResponse> listAll() {
        return eventRepository.listAllOrdered().stream().map(EventResponse::from).toList();
    }

    private void validateWindow(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 255) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }
    }

    private void validateTimes(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || endTime.isBefore(startTime)) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }
    }
}
