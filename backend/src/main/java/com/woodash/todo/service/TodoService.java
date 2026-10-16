package com.woodash.todo.service;

import java.util.List;
import java.util.UUID;

import com.woodash.auth.security.CurrentUser;
import com.woodash.common.ErrorCode;
import com.woodash.todo.entity.Todo;
import com.woodash.todo.repository.TodoRepository;
import com.woodash.todo.rest.dto.TodoCreateRequest;
import com.woodash.todo.rest.dto.TodoResponse;
import com.woodash.todo.rest.dto.TodoUpdateRequest;
import com.woodash.user.entity.User;
import com.woodash.user.repository.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TodoService {

    @Inject
    TodoRepository todoRepository;

    @Inject
    UserRepository userRepository;

    public List<TodoResponse> listByUser(UUID userId, Boolean completed, CurrentUser currentUser) {
        if (userRepository.findByIdOptional(userId).isEmpty()) {
            ErrorCode.NOT_FOUND.throwException();
        }
        currentUser.requireOwnerOrAdmin(userId);
        List<Todo> todos = completed == null ? todoRepository.listByUser(userId)
                : todoRepository.listByUserAndCompleted(userId, completed);
        return todos.stream().map(TodoResponse::from).toList();
    }

    @Transactional
    public TodoResponse create(TodoCreateRequest request, UUID userId) {
        validateName(request == null ? null : request.getName());
        User user = userRepository.findByIdOptional(userId).orElseThrow(ErrorCode.NOT_FOUND::getException);

        Todo todo = new Todo();
        todo.setName(request.getName());
        todo.setDescription(request.getDescription());
        todo.setCompleted(Boolean.TRUE.equals(request.getCompleted()));
        todo.setUser(user);
        todoRepository.persistAndFlush(todo);
        return TodoResponse.from(todo);
    }

    @Transactional
    public TodoResponse update(UUID id, TodoUpdateRequest request, CurrentUser currentUser) {
        if (request == null || !request.hasUpdates()) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }
        Todo todo = todoRepository.findByIdOptional(id).orElseThrow(ErrorCode.NOT_FOUND::getException);
        currentUser.requireOwnerOrAdmin(todo.getUser().getId());

        if (request.hasName()) {
            validateName(request.getName());
            todo.setName(request.getName());
        }
        if (request.hasDescription()) {
            todo.setDescription(request.getDescription());
        }
        if (request.hasCompleted()) {
            if (request.getCompleted() == null) {
                ErrorCode.INVALID_REQUEST_BODY.throwException();
            }
            todo.setCompleted(request.getCompleted());
        }

        todoRepository.flush();
        return TodoResponse.from(todo);
    }

    @Transactional
    public void delete(UUID id, CurrentUser currentUser) {
        Todo todo = todoRepository.findByIdOptional(id).orElseThrow(ErrorCode.NOT_FOUND::getException);
        currentUser.requireOwnerOrAdmin(todo.getUser().getId());
        todoRepository.delete(todo);
    }

    public List<TodoResponse> listAll() {
        return todoRepository.listAllOrdered().stream().map(TodoResponse::from).toList();
    }

    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 255) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }
    }
}
