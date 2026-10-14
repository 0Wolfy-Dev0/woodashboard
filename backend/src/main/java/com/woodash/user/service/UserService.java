package com.woodash.user.service;

import java.util.Optional;

import com.woodash.user.entity.User;
import com.woodash.user.repository.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    public boolean loginExist(String login) {
        Optional<User> user = userRepository.findByLogin(login);
        return user.isPresent();
    }

    public void createUser(String login, String password) {
        userRepository.createUser(login, password);
    }

    public User getUserByLogin(String login) {
        return userRepository.findByLogin(login).get();
    }
}
