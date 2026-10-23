package com.woodash.group.service;

import java.util.List;
import java.util.UUID;

import com.woodash.auth.security.CurrentUser;
import com.woodash.common.ErrorCode;
import com.woodash.group.entity.Group;
import com.woodash.group.entity.GroupMember;
import com.woodash.group.repository.GroupMemberRepository;
import com.woodash.group.repository.GroupRepository;
import com.woodash.group.rest.dto.GroupCreateRequest;
import com.woodash.group.rest.dto.GroupMemberRequest;
import com.woodash.group.rest.dto.GroupResponse;
import com.woodash.group.rest.dto.GroupUpdateRequest;
import com.woodash.user.entity.User;
import com.woodash.user.repository.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class GroupService {

    @Inject
    GroupRepository groupRepository;

    @Inject
    GroupMemberRepository groupMemberRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    GroupAccessService groupAccessService;

    @Transactional
    public List<GroupResponse> listForUser(CurrentUser currentUser) {
        return groupRepository.listForUser(currentUser.getUserId()).stream()
                .map(GroupResponse::from)
                .toList();
    }

    @Transactional
    public GroupResponse get(UUID id, CurrentUser currentUser) {
        return GroupResponse.from(groupAccessService.requireVisibleGroup(id, currentUser));
    }

    @Transactional
    public GroupResponse create(GroupCreateRequest request, UUID userId) {
        validateName(request == null ? null : request.getName());
        User owner = userRepository.findByIdOptional(userId).orElseThrow(ErrorCode.NOT_FOUND::getException);

        Group group = new Group();
        group.setName(request.getName().trim());
        group.setDescription(request.getDescription());
        group.setOwner(owner);
        groupRepository.persistAndFlush(group);

        GroupMember ownerMembership = new GroupMember();
        ownerMembership.setSharedGroup(group);
        ownerMembership.setUser(owner);
        groupMemberRepository.persist(ownerMembership);
        group.getMembers().add(ownerMembership);
        groupRepository.flush();

        return GroupResponse.from(group);
    }

    @Transactional
    public GroupResponse update(UUID id, GroupUpdateRequest request, CurrentUser currentUser) {
        if (request == null || !request.hasUpdates()) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }

        Group group = groupAccessService.requireGroup(id);
        groupAccessService.requireOwnerOrAdmin(group, currentUser);

        if (request.hasName()) {
            validateName(request.getName());
            group.setName(request.getName().trim());
        }
        if (request.hasDescription()) {
            group.setDescription(request.getDescription());
        }
        groupRepository.flush();
        return GroupResponse.from(group);
    }

    @Transactional
    public void delete(UUID id, CurrentUser currentUser) {
        Group group = groupAccessService.requireGroup(id);
        groupAccessService.requireOwnerOrAdmin(group, currentUser);
        groupRepository.delete(group);
    }

    @Transactional
    public GroupResponse addMember(UUID id, GroupMemberRequest request, CurrentUser currentUser) {
        Group group = groupAccessService.requireGroup(id);
        groupAccessService.requireOwnerOrAdmin(group, currentUser);

        if (request == null || request.getLogin() == null || request.getLogin().isBlank()) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }

        User user = userRepository.findByLogin(request.getLogin().trim())
                .orElseThrow(ErrorCode.NOT_FOUND::getException);
        if (groupMemberRepository.existsFor(id, user.getId())) {
            ErrorCode.GROUP_MEMBER_ALREADY_EXISTS.throwException();
        }

        GroupMember membership = new GroupMember();
        membership.setSharedGroup(group);
        membership.setUser(user);
        groupMemberRepository.persist(membership);
        group.getMembers().add(membership);
        groupRepository.flush();

        return GroupResponse.from(group);
    }

    @Transactional
    public void removeMember(UUID id, UUID memberId, CurrentUser currentUser) {
        Group group = groupAccessService.requireGroup(id);
        if (group.getOwner().getId().equals(memberId)) {
            ErrorCode.CANNOT_REMOVE_GROUP_OWNER.throwException();
        }

        if (!currentUser.isAdmin() && !currentUser.getUserId().equals(memberId)) {
            groupAccessService.requireOwnerOrAdmin(group, currentUser);
        }


        GroupMember membership = groupMemberRepository.findFor(id, memberId)
                .orElseThrow(ErrorCode.NOT_FOUND::getException);
        groupMemberRepository.delete(membership);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 100) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }
    }
}
