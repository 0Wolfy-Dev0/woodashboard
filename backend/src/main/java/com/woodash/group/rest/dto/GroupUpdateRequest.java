package com.woodash.group.rest.dto;

public class GroupUpdateRequest {
    private String name;
    private String description;
    private boolean nameProvided;
    private boolean descriptionProvided;

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

    public boolean hasUpdates() {
        return nameProvided || descriptionProvided;
    }

    public boolean hasName() {
        return nameProvided;
    }

    public boolean hasDescription() {
        return descriptionProvided;
    }
}
