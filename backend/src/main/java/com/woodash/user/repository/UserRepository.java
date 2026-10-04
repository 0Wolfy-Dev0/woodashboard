package com.woodash.user.repository;

import com.woodash.user.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
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
}
