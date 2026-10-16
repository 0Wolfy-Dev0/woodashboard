package com.woodash.user.service;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import com.woodash.user.entity.User;
import com.woodash.user.repository.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    public boolean loginExist(String login) {
        Optional<User> user = userRepository.findByLogin(login);
        return user.isPresent();
    }

    public User createUser(String login, String password) {
        return userRepository.createUser(login, password);
    }

    public User getUserByLogin(String login) {
        return userRepository.findByLogin(login).orElse(null);
    }

    public Optional<User> getUserById(UUID id) {
        return userRepository.findByIdOptional(id);
    }

    public List<User> listAll() {
        return userRepository.listAll();
    }

    @Transactional
    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }
}
