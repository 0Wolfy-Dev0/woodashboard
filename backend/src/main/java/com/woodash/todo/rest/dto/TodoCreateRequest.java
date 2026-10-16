package com.woodash.todo.rest.dto;

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
}
