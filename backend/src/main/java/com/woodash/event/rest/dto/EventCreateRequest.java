package com.woodash.event.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.woodash.event.entity.RecurrenceUnit;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EventCreateRequest {
    private String name;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean allDay;
    private RecurrenceUnit recurrenceUnit;
    private Integer recurrenceInterval;
    private LocalDateTime recurrenceEnd;
    private UUID groupId;
}
