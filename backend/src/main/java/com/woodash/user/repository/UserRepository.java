package com.woodash.user.repository;

import com.woodash.user.entity.Role;
import com.woodash.user.entity.User;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<User, UUID> {

    public Optional<User> findByLogin(String login) {
        return find("login", login).firstResultOptional();
    }

    public boolean existsByLogin(String login) {
        return count("login", login) > 0;
    }

    @Transactional
    public User createUser(String login, String passwordHash) {
        User user = new User();
        user.setLogin(login);
        user.setPasswordHash(passwordHash);
        user.setRole(Role.USER);
        this.persistAndFlush(user);
        return user;
    }

    public List<User> listAll() {
        return list("order by creationTime");
    }
}
