package com.woodash.event.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.woodash.event.entity.Event;

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
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime creationTime;
    private LocalDateTime lastModificationTime;

    public static EventResponse from(Event event) {
        return new EventResponse(event.getId(), event.getName(), event.getDescription(), event.getUser().getId(),
                event.getStartTime(), event.getEndTime(), event.getCreationTime(), event.getLastModificationTime());
    }
}
