package com.woodash.user.repository;

import com.woodash.common.PageRequest;
import com.woodash.common.PageResponse;
import com.woodash.user.entity.Role;
import com.woodash.user.entity.User;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Locale;
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
    public User createUser(String login, String passwordHash) {
        User user = new User();
        user.setLogin(login);
        user.setPasswordHash(passwordHash);
        user.setRole(Role.USER);
        this.persistAndFlush(user);
        return user;
    }

    public PageResponse<User> searchPage(String search, PageRequest page) {
        PanacheQuery<User> paged = search == null
                ? find("order by creationTime")
                : find("lower(login) like ?1 order by creationTime",
                        "%" + search.toLowerCase(Locale.ROOT) + "%");
        long total = paged.count();
        return PageResponse.of(paged.page(page.page(), page.size()).list(), page.page(), page.size(), total);
    }
}
