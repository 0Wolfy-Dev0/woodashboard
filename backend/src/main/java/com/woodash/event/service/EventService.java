package com.woodash.event.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.woodash.auth.security.CurrentUser;
import com.woodash.common.ErrorCode;
import com.woodash.event.entity.Event;
import com.woodash.event.entity.RecurrenceUnit;
import com.woodash.event.repository.EventRepository;
import com.woodash.event.rest.dto.EventCreateRequest;
import com.woodash.event.rest.dto.EventResponse;
import com.woodash.event.rest.dto.EventUpdateRequest;
import com.woodash.group.entity.Group;
import com.woodash.group.service.GroupAccessService;
import com.woodash.user.repository.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class EventService {

    private static final int MAX_OCCURRENCES_PER_EVENT = 10000;
    private static final int MAX_RECURRENCE_INTERVAL = 1000;
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);

    @Inject
    EventRepository eventRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    GroupAccessService groupAccessService;

    public List<EventResponse> listAccessible(UUID userId, UUID groupId, LocalDateTime from, LocalDateTime to,
            CurrentUser currentUser) {
        validateWindow(from, to);
        List<Event> events;
        if (groupId != null) {
            groupAccessService.requireVisibleGroup(groupId, currentUser);
            events = eventRepository.listByGroup(groupId, null, null);
        } else {
            events = eventRepository.listAccessible(userId, null, null);
        }

        // Without a window we return the series definitions themselves. Expanding an endless
        // recurring series here would never terminate.
        if (from == null && to == null) {
            return events.stream().map(EventResponse::from).toList();
        }

        LocalDateTime windowStart = from != null ? from : to.minusYears(10);
        LocalDateTime windowEnd = to != null ? to : from.plusYears(10);
        List<EventOccurrence> occurrences = new ArrayList<>();
        for (Event event : events) {
            if (event.getRecurrenceUnit() == null) {
                if (overlaps(event.getStartTime(), event.getEndTime(), windowStart, windowEnd)) {
                    occurrences.add(new EventOccurrence(event, event.getStartTime(), event.getEndTime()));
                }
            } else {
                occurrences.addAll(expand(event, windowStart, windowEnd));
            }
        }

        occurrences.sort(Comparator.comparing(EventOccurrence::startTime)
                .thenComparing(occurrence -> occurrence.event().getName(), String.CASE_INSENSITIVE_ORDER));
        return occurrences.stream()
                .map(occurrence -> EventResponse.from(occurrence.event(), occurrence.startTime(),
                        occurrence.endTime()))
                .toList();
    }

    @Transactional
    public EventResponse create(EventCreateRequest request, CurrentUser currentUser) {
        if (request == null) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }
        validateName(request.getName());

        boolean allDay = Boolean.TRUE.equals(request.getAllDay());
        LocalDateTime startTime = normalizeStart(request.getStartTime(), allDay);
        LocalDateTime endTime = normalizeEnd(request.getEndTime(), allDay);
        validateTimes(startTime, endTime);
        Recurrence recurrence = parseRecurrence(request.getRecurrenceUnit(), request.getRecurrenceInterval(),
                request.getRecurrenceEnd(), allDay, startTime);

        UUID userId = currentUser.getUserId();
        var user = userRepository.findByIdOptional(userId).orElseThrow(ErrorCode.NOT_FOUND::getException);
        Group group = request.getGroupId() == null ? null
                : groupAccessService.requireVisibleGroup(request.getGroupId(), currentUser);

        Event event = new Event();
        event.setName(request.getName().trim());
        event.setDescription(request.getDescription());
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setAllDay(allDay);
        event.setRecurrenceUnit(recurrence.unit());
        event.setRecurrenceInterval(recurrence.interval());
        event.setRecurrenceEnd(recurrence.end());
        event.setUser(user);
        event.setSharedGroup(group);
        eventRepository.persistAndFlush(event);
        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse update(UUID id, EventUpdateRequest request, CurrentUser currentUser) {
        if (request == null || !request.hasUpdates()) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }
        Event event = eventRepository.findByIdOptional(id).orElseThrow(ErrorCode.NOT_FOUND::getException);
        groupAccessService.requireItemAccess(event.getUser().getId(), event.getSharedGroup(), currentUser);

        if (request.hasAllDay() && request.getAllDay() == null) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }
        boolean allDay = request.hasAllDay() ? request.getAllDay() : event.isAllDay();
        String name = request.hasName() ? request.getName() : event.getName();
        LocalDateTime startTime = normalizeStart(
                request.hasStartTime() ? request.getStartTime() : event.getStartTime(), allDay);
        LocalDateTime endTime = normalizeEnd(
                request.hasEndTime() ? request.getEndTime() : event.getEndTime(), allDay);
        validateName(name);
        validateTimes(startTime, endTime);

        RecurrenceUnit recurrenceUnit = request.hasRecurrenceUnit() ? request.getRecurrenceUnit()
                : event.getRecurrenceUnit();
        Integer recurrenceInterval = request.hasRecurrenceInterval() ? request.getRecurrenceInterval()
                : event.getRecurrenceInterval();
        LocalDateTime recurrenceEnd = request.hasRecurrenceEnd() ? request.getRecurrenceEnd()
                : event.getRecurrenceEnd();
        if (request.hasRecurrenceUnit() && recurrenceUnit == null) {
            recurrenceInterval = null;
            recurrenceEnd = null;
        }
        Recurrence recurrence = parseRecurrence(recurrenceUnit, recurrenceInterval, recurrenceEnd, allDay, startTime);

        event.setName(name.trim());
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setAllDay(allDay);
        event.setRecurrenceUnit(recurrence.unit());
        event.setRecurrenceInterval(recurrence.interval());
        event.setRecurrenceEnd(recurrence.end());
        if (request.hasDescription()) {
            event.setDescription(request.getDescription());
        }
        eventRepository.flush();
        return EventResponse.from(event);
    }

    @Transactional
    public void delete(UUID id, CurrentUser currentUser) {
        Event event = eventRepository.findByIdOptional(id).orElseThrow(ErrorCode.NOT_FOUND::getException);
        groupAccessService.requireItemAccess(event.getUser().getId(), event.getSharedGroup(), currentUser);
        eventRepository.delete(event);
    }

    public List<EventResponse> listAll() {
        return eventRepository.listAllOrdered().stream().map(EventResponse::from).toList();
    }

    private List<EventOccurrence> expand(Event event, LocalDateTime from, LocalDateTime to) {
        Duration duration = Duration.between(event.getStartTime(), event.getEndTime());
        RecurrenceUnit unit = event.getRecurrenceUnit();
        int interval = event.getRecurrenceInterval() == null ? 1 : event.getRecurrenceInterval();
        LocalDateTime seriesEnd = event.getRecurrenceEnd();
        LocalDateTime baseStart = event.getStartTime();

        long index = firstIndex(baseStart, from.minus(duration), unit, interval);
        List<EventOccurrence> occurrences = new ArrayList<>();
        for (int count = 0; count < MAX_OCCURRENCES_PER_EVENT; count++, index++) {
            LocalDateTime occurrenceStart = shift(baseStart, unit, (long) interval * index);
            if (occurrenceStart.isAfter(to)) {
                break;
            }
            if (seriesEnd != null && occurrenceStart.isAfter(seriesEnd)) {
                break;
            }
            LocalDateTime occurrenceEnd = occurrenceStart.plus(duration);
            if (!occurrenceEnd.isBefore(from)) {
                occurrences.add(new EventOccurrence(event, occurrenceStart, occurrenceEnd));
            }
        }
        return occurrences;
    }

    /** Estimate how many intervals fit before the window to avoid walking years of occurrences. */
    private long firstIndex(LocalDateTime baseStart, LocalDateTime target, RecurrenceUnit unit, int interval) {
        if (!target.isAfter(baseStart)) {
            return 0;
        }
        long estimate = switch (unit) {
            case DAYS -> ChronoUnit.DAYS.between(baseStart, target) / interval;
            case WEEKS -> ChronoUnit.DAYS.between(baseStart, target) / (7L * interval);
            case MONTHS -> ChronoUnit.MONTHS.between(baseStart, target) / interval;
            case YEARS -> ChronoUnit.YEARS.between(baseStart, target) / interval;
        };
        return Math.max(0, estimate - 1);
    }

    private LocalDateTime shift(LocalDateTime base, RecurrenceUnit unit, long intervals) {
        return switch (unit) {
            case DAYS -> base.plusDays(intervals);
            case WEEKS -> base.plusWeeks(intervals);
            case MONTHS -> base.plusMonths(intervals);
            case YEARS -> base.plusYears(intervals);
        };
    }

    private Recurrence parseRecurrence(RecurrenceUnit unit, Integer interval, LocalDateTime recurrenceEnd,
            boolean allDay, LocalDateTime startTime) {
        if (unit == null) {
            if (interval != null || recurrenceEnd != null) {
                ErrorCode.INVALID_REQUEST_BODY.throwException();
            }
            return Recurrence.none();
        }

        int periods = interval == null ? 1 : interval;
        if (periods < 1 || periods > MAX_RECURRENCE_INTERVAL) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }

        LocalDateTime normalizedEnd = recurrenceEnd == null ? null
                : allDay ? recurrenceEnd.toLocalDate().atTime(END_OF_DAY) : recurrenceEnd;
        if (normalizedEnd != null && normalizedEnd.isBefore(startTime)) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }
        return new Recurrence(unit, periods, normalizedEnd);
    }

    private boolean overlaps(LocalDateTime start, LocalDateTime end, LocalDateTime from, LocalDateTime to) {
        return !end.isBefore(from) && !start.isAfter(to);
    }

    private LocalDateTime normalizeStart(LocalDateTime value, boolean allDay) {
        return value == null ? null : allDay ? value.toLocalDate().atStartOfDay() : value;
    }

    private LocalDateTime normalizeEnd(LocalDateTime value, boolean allDay) {
        return value == null ? null : allDay ? value.toLocalDate().atTime(END_OF_DAY) : value;
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

    private record EventOccurrence(Event event, LocalDateTime startTime, LocalDateTime endTime) {
    }

    private record Recurrence(RecurrenceUnit unit, Integer interval, LocalDateTime end) {
        static Recurrence none() {
            return new Recurrence(null, null, null);
        }
    }
}
