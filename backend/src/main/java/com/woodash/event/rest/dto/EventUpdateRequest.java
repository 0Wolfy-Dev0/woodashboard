package com.woodash.event.rest.dto;

import java.time.LocalDateTime;

import com.woodash.event.entity.RecurrenceUnit;

public class EventUpdateRequest {
    private String name;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean allDay;
    private RecurrenceUnit recurrenceUnit;
    private Integer recurrenceInterval;
    private LocalDateTime recurrenceEnd;
    private boolean nameProvided;
    private boolean descriptionProvided;
    private boolean startTimeProvided;
    private boolean endTimeProvided;
    private boolean allDayProvided;
    private boolean recurrenceUnitProvided;
    private boolean recurrenceIntervalProvided;
    private boolean recurrenceEndProvided;

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

    public Boolean getAllDay() {
        return allDay;
    }

    public void setAllDay(Boolean allDay) {
        this.allDay = allDay;
        this.allDayProvided = true;
    }

    public RecurrenceUnit getRecurrenceUnit() {
        return recurrenceUnit;
    }

    public void setRecurrenceUnit(RecurrenceUnit recurrenceUnit) {
        this.recurrenceUnit = recurrenceUnit;
        this.recurrenceUnitProvided = true;
    }

    public Integer getRecurrenceInterval() {
        return recurrenceInterval;
    }

    public void setRecurrenceInterval(Integer recurrenceInterval) {
        this.recurrenceInterval = recurrenceInterval;
        this.recurrenceIntervalProvided = true;
    }

    public LocalDateTime getRecurrenceEnd() {
        return recurrenceEnd;
    }

    public void setRecurrenceEnd(LocalDateTime recurrenceEnd) {
        this.recurrenceEnd = recurrenceEnd;
        this.recurrenceEndProvided = true;
    }

    public boolean hasUpdates() {
        return nameProvided || descriptionProvided || startTimeProvided || endTimeProvided || allDayProvided
                || recurrenceUnitProvided || recurrenceIntervalProvided || recurrenceEndProvided;
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

    public boolean hasAllDay() {
        return allDayProvided;
    }

    public boolean hasRecurrenceUnit() {
        return recurrenceUnitProvided;
    }

    public boolean hasRecurrenceInterval() {
        return recurrenceIntervalProvided;
    }

    public boolean hasRecurrenceEnd() {
        return recurrenceEndProvided;
    }
}
