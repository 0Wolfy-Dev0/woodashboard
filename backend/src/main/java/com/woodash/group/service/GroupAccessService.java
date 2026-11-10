package com.woodash.group.service;

import java.util.UUID;

import com.woodash.auth.security.CurrentUser;
import com.woodash.common.ErrorCode;
import com.woodash.group.entity.Group;
import com.woodash.group.repository.GroupMemberRepository;
import com.woodash.group.repository.GroupRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GroupAccessService {

    @Inject
    GroupRepository groupRepository;

    @Inject
    GroupMemberRepository groupMemberRepository;

    public Group requireGroup(UUID groupId) {
        return groupRepository.findByIdOptional(groupId).orElseThrow(ErrorCode.NOT_FOUND::getException);
    }

    public Group requireVisibleGroup(UUID groupId, CurrentUser currentUser) {
        Group group = requireGroup(groupId);
        if (!currentUser.isAdmin() && !isMember(groupId, currentUser.getUserId())) {
            ErrorCode.FORBIDDEN.throwException();
        }
        return group;
    }

    public void requireOwnerOrAdmin(Group group, CurrentUser currentUser) {
        if (!currentUser.isAdmin() && !group.getOwner().getId().equals(currentUser.getUserId())) {
            ErrorCode.FORBIDDEN.throwException();
        }
    }

    public void requireItemAccess(UUID creatorId, Group group, CurrentUser currentUser) {
        if (currentUser.isAdmin()) {
            return;
        }
        if (group == null) {
            if (currentUser.getUserId().equals(creatorId)) {
                return;
            }
        } else if (isMember(group.getId(), currentUser.getUserId())) {
            return;
        }
        ErrorCode.FORBIDDEN.throwException();
    }

    public boolean isMember(UUID groupId, UUID userId) {
        return groupMemberRepository.existsFor(groupId, userId);
    }
}
