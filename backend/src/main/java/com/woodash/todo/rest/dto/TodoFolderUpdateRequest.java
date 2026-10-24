package com.woodash.todo.rest.dto;

public class TodoFolderUpdateRequest {
    private String name;
    private boolean nameProvided;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.nameProvided = true;
    }

    public boolean hasUpdates() {
        return nameProvided;
    }

    public boolean hasName() {
        return nameProvided;
    }
}
