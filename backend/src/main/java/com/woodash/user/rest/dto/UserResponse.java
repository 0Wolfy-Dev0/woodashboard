package com.woodash.user.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.woodash.user.entity.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
public class UserResponse {
    UUID id;
    String login;
    Role role;
    LocalDateTime creationTime;
    LocalDateTime lastModificationTime;
}
