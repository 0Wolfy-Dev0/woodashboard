package com.woodash.todo.repository;

import com.woodash.todo.entity.TodoFolder;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TodoFolderRepository implements PanacheRepositoryBase<TodoFolder, UUID> {

    public List<TodoFolder> listForUser(UUID userId) {
        return list("user.id = ?1 order by lower(name)", userId);
    }

    public Optional<TodoFolder> findForUser(UUID id, UUID userId) {
        return find("id = ?1 and user.id = ?2", id, userId).firstResultOptional();
    }

    public boolean existsForUser(UUID userId, String name) {
        return count("user.id = ?1 and lower(name) = lower(?2)", userId, name) > 0;
    }

    public boolean existsForUserExcept(UUID userId, String name, UUID exceptId) {
        return count("user.id = ?1 and lower(name) = lower(?2) and id <> ?3", userId, name, exceptId) > 0;
    }
}
