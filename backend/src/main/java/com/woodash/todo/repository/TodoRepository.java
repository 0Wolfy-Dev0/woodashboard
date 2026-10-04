package com.woodash.todo.repository;

import com.woodash.todo.entity.Todo;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TodoRepository implements PanacheRepositoryBase<Todo, UUID> {

    public List<Todo> listByUser(UUID userId) {
        return list("user.id = ?1 order by completed, creationTime", userId);
    }
}
