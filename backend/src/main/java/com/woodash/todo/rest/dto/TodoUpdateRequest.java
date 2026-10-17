package com.woodash.todo.rest.dto;

public class TodoUpdateRequest {
    private String name;
    private String description;
    private Boolean completed;
    private boolean nameProvided;
    private boolean descriptionProvided;
    private boolean completedProvided;

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

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
        this.completedProvided = true;
    }

    public boolean hasUpdates() {
        return nameProvided || descriptionProvided || completedProvided;
    }

    public boolean hasName() {
        return nameProvided;
    }

    public boolean hasDescription() {
        return descriptionProvided;
    }

    public boolean hasCompleted() {
        return completedProvided;
    }
}
