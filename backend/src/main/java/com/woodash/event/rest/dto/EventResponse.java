package com.woodash.event.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.woodash.event.entity.Event;
import com.woodash.event.entity.RecurrenceUnit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID userId;
    private UUID groupId;
    private boolean allDay;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime seriesStartTime;
    private LocalDateTime seriesEndTime;
    private RecurrenceUnit recurrenceUnit;
    private Integer recurrenceInterval;
    private LocalDateTime recurrenceEnd;
    private LocalDateTime creationTime;
    private LocalDateTime lastModificationTime;

    public static EventResponse from(Event event) {
        return from(event, event.getStartTime(), event.getEndTime());
    }

    /** Builds a response for one occurrence while keeping the series' base times. */
    public static EventResponse from(Event event, LocalDateTime occurrenceStart, LocalDateTime occurrenceEnd) {
        UUID groupId = event.getSharedGroup() == null ? null : event.getSharedGroup().getId();
        return new EventResponse(event.getId(), event.getName(), event.getDescription(), event.getUser().getId(),
                groupId, event.isAllDay(), occurrenceStart, occurrenceEnd, event.getStartTime(), event.getEndTime(),
                event.getRecurrenceUnit(), event.getRecurrenceInterval(), event.getRecurrenceEnd(),
                event.getCreationTime(), event.getLastModificationTime());
    }
}
