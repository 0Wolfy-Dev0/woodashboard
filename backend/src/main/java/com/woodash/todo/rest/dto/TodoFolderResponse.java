package com.woodash.todo.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.woodash.todo.entity.TodoFolder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TodoFolderResponse {
    private UUID id;
    private String name;
    private UUID userId;
    private LocalDateTime creationTime;
    private LocalDateTime lastModificationTime;

    public static TodoFolderResponse from(TodoFolder folder) {
        return new TodoFolderResponse(folder.getId(), folder.getName(), folder.getUser().getId(),
                folder.getCreationTime(), folder.getLastModificationTime());
    }
}
