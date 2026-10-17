package com.woodash.todo.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.woodash.todo.entity.Todo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TodoResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID userId;
    private boolean completed;
    private LocalDateTime creationTime;
    private LocalDateTime lastModificationTime;

    public static TodoResponse from(Todo todo) {
        return new TodoResponse(todo.getId(), todo.getName(), todo.getDescription(), todo.getUser().getId(),
                todo.isCompleted(), todo.getCreationTime(), todo.getLastModificationTime());
    }
}
