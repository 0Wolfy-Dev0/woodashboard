package com.woodash.todo.service;

import java.util.List;
import java.util.UUID;

import com.woodash.auth.security.CurrentUser;
import com.woodash.common.ErrorCode;
import com.woodash.todo.entity.TodoFolder;
import com.woodash.todo.repository.TodoFolderRepository;
import com.woodash.todo.rest.dto.TodoFolderCreateRequest;
import com.woodash.todo.rest.dto.TodoFolderResponse;
import com.woodash.todo.rest.dto.TodoFolderUpdateRequest;
import com.woodash.user.entity.User;
import com.woodash.user.repository.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TodoFolderService {

    @Inject
    TodoFolderRepository todoFolderRepository;

    @Inject
    UserRepository userRepository;

    public List<TodoFolderResponse> list(CurrentUser currentUser) {
        return todoFolderRepository.listForUser(currentUser.getUserId()).stream()
                .map(TodoFolderResponse::from)
                .toList();
    }

    @Transactional
    public TodoFolderResponse create(TodoFolderCreateRequest request, CurrentUser currentUser) {
        String name = validateName(request == null ? null : request.getName());
        if (todoFolderRepository.existsForUser(currentUser.getUserId(), name)) {
            ErrorCode.TODO_FOLDER_ALREADY_EXISTS.throwException();
        }

        User user = userRepository.findByIdOptional(currentUser.getUserId())
                .orElseThrow(ErrorCode.NOT_FOUND::getException);
        TodoFolder folder = new TodoFolder();
        folder.setName(name);
        folder.setUser(user);
        todoFolderRepository.persistAndFlush(folder);
        return TodoFolderResponse.from(folder);
    }

    @Transactional
    public TodoFolderResponse update(UUID id, TodoFolderUpdateRequest request, CurrentUser currentUser) {
        if (request == null || !request.hasUpdates()) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }

        TodoFolder folder = requireOwnedFolder(id, currentUser.getUserId());
        if (request.hasName()) {
            String name = validateName(request.getName());
            if (todoFolderRepository.existsForUserExcept(currentUser.getUserId(), name, id)) {
                ErrorCode.TODO_FOLDER_ALREADY_EXISTS.throwException();
            }
            folder.setName(name);
        }
        todoFolderRepository.flush();
        return TodoFolderResponse.from(folder);
    }

    @Transactional
    public void delete(UUID id, CurrentUser currentUser) {
        todoFolderRepository.delete(requireOwnedFolder(id, currentUser.getUserId()));
    }

    public TodoFolder requireOwnedFolder(UUID id, UUID userId) {
        return todoFolderRepository.findForUser(id, userId).orElseThrow(ErrorCode.NOT_FOUND::getException);
    }

    private String validateName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 100) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }
        return name.trim();
    }
}
