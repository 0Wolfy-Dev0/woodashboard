package com.woodash.group.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.woodash.group.entity.Group;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID ownerId;
    private String ownerLogin;
    private long memberCount;
    private LocalDateTime creationTime;
    private LocalDateTime lastModificationTime;

    public static GroupResponse from(Group group) {
        return new GroupResponse(group.getId(), group.getName(), group.getDescription(),
                group.getOwner().getId(), group.getOwner().getLogin(), group.getMembers().size(),
                group.getCreationTime(), group.getLastModificationTime());
    }
}
