package com.woodash.group.rest.dto;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.woodash.group.entity.Group;
import com.woodash.user.rest.dto.UserResponse;

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
    private List<UserResponse> members;
    private LocalDateTime creationTime;
    private LocalDateTime lastModificationTime;

    public static GroupResponse from(Group group) {
        List<UserResponse> members = group.getMembers().stream()
                .map(member -> member.getUser())
                .sorted(Comparator.comparing(user -> user.getLogin().toLowerCase()))
                .map(user -> new UserResponse(user.getId(), user.getLogin(), user.getRole(),
                        user.getCreationTime(), user.getLastModificationTime()))
                .toList();

        return new GroupResponse(group.getId(), group.getName(), group.getDescription(),
                group.getOwner().getId(), group.getOwner().getLogin(), members,
                group.getCreationTime(), group.getLastModificationTime());
    }
}
