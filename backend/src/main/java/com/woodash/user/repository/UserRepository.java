package com.woodash.user.repository;

import com.woodash.user.entity.Role;
import com.woodash.user.entity.User;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;
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
    public void createUser(String login, String password) {
        User user = new User();
        user.setLogin(login);
        user.setPasswordHash(BcryptUtil.bcryptHash(password));
        user.setRole(Role.USER);
        this.persist(user);
    }
}
