package com.woodash.todo.repository;

import com.woodash.todo.entity.Todo;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TodoRepository implements PanacheRepositoryBase<Todo, UUID> {

    public List<Todo> listByUser(UUID userId) {
        return list("user.id = ?1 order by completed, creationTime", userId);
    }

    public List<Todo> listByUserAndCompleted(UUID userId, boolean completed) {
        return list("user.id = ?1 and completed = ?2 order by creationTime", userId, completed);
    }

    public List<Todo> listAccessible(UUID userId, Boolean completed, UUID groupId) {
        StringBuilder query = new StringBuilder(
                "select distinct t from Todo t left join t.sharedGroup g left join g.members gm"
                        + " where ((t.sharedGroup is null and t.user.id = ?1)"
                        + " or (t.sharedGroup is not null and gm.user.id = ?1))");
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);
        appendFilters(query, parameters, completed, groupId);
        query.append(" order by t.completed, t.creationTime");
        return list(query.toString(), parameters.toArray());
    }

    public List<Todo> listByGroup(UUID groupId, Boolean completed) {
        StringBuilder query = new StringBuilder("select t from Todo t where t.sharedGroup.id = ?1");
        List<Object> parameters = new ArrayList<>();
        parameters.add(groupId);
        appendFilters(query, parameters, completed, null);
        query.append(" order by t.completed, t.creationTime");
        return list(query.toString(), parameters.toArray());
    }

    public List<Todo> listAllOrdered() {
        return list("order by completed, creationTime");
    }

    private void appendFilters(StringBuilder query, List<Object> parameters, Boolean completed, UUID groupId) {
        if (completed != null) {
            query.append(" and t.completed = ?").append(parameters.size() + 1);
            parameters.add(completed);
        }
        if (groupId != null) {
            query.append(" and t.sharedGroup.id = ?").append(parameters.size() + 1);
            parameters.add(groupId);
        }
    }
}
