package com.woodash.todo.rest.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TodoCreateRequest {
    private String name;
    private String description;
    private Boolean completed;
    private UUID groupId;
    private UUID folderId;
}
