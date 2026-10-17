package com.woodash.event.rest.dto;

import java.time.LocalDateTime;

public class EventUpdateRequest {
    private String name;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean nameProvided;
    private boolean descriptionProvided;
    private boolean startTimeProvided;
    private boolean endTimeProvided;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.nameProvided = true;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.descriptionProvided = true;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        this.startTimeProvided = true;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        this.endTimeProvided = true;
    }

    public boolean hasUpdates() {
        return nameProvided || descriptionProvided || startTimeProvided || endTimeProvided;
    }

    public boolean hasName() {
        return nameProvided;
    }

    public boolean hasDescription() {
        return descriptionProvided;
    }

    public boolean hasStartTime() {
        return startTimeProvided;
    }

    public boolean hasEndTime() {
        return endTimeProvided;
    }
}
