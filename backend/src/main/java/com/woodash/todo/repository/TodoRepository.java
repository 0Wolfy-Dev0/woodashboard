package com.woodash.todo.repository;

import com.woodash.common.PageRequest;
import com.woodash.common.PageResponse;
import com.woodash.todo.entity.Todo;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class TodoRepository implements PanacheRepositoryBase<Todo, UUID> {

    public List<Todo> listByUser(UUID userId) {
        return list("select t from Todo t left join fetch t.user"
                + " where t.user.id = ?1 order by t.completed, t.creationTime", userId);
    }

    public List<Todo> listByUserAndCompleted(UUID userId, boolean completed) {
        return list("select t from Todo t left join fetch t.user"
                + " where t.user.id = ?1 and t.completed = ?2 order by t.creationTime", userId, completed);
    }

    public List<Todo> listAccessible(UUID userId, Boolean completed, UUID groupId, UUID folderId) {
        StringBuilder query = new StringBuilder(
                "select distinct t from Todo t left join fetch t.user"
                        + " left join t.sharedGroup g left join g.members gm"
                        + " where ((t.sharedGroup is null and t.user.id = ?1)"
                        + " or (t.sharedGroup is not null and gm.user.id = ?1))");
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);
        appendFilters(query, parameters, completed, groupId, folderId);
        query.append(" order by t.completed, t.creationTime");
        return list(query.toString(), parameters.toArray());
    }

    public List<Todo> listByGroup(UUID groupId, Boolean completed) {
        StringBuilder query = new StringBuilder(
                "select t from Todo t left join fetch t.user where t.sharedGroup.id = ?1");
        List<Object> parameters = new ArrayList<>();
        parameters.add(groupId);
        appendFilters(query, parameters, completed, null, null);
        query.append(" order by t.completed, t.creationTime");
        return list(query.toString(), parameters.toArray());
    }

    public PageResponse<Todo> searchAllPage(String search, PageRequest page) {
        StringBuilder query = new StringBuilder(
                "select t from Todo t join fetch t.user u where 1 = 1");
        List<Object> parameters = new ArrayList<>();
        if (search != null) {
            String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
            int first = parameters.size() + 1;
            query.append(" and (lower(t.name) like ?").append(first)
                    .append(" or lower(coalesce(t.description, '')) like ?").append(first + 1)
                    .append(" or lower(u.login) like ?").append(first + 2).append(")");
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
        }
        query.append(" order by t.creationTime");
        PanacheQuery<Todo> paged = find(query.toString(), parameters.toArray());
        long total = paged.count();
        return PageResponse.of(paged.page(page.page(), page.size()).list(), page.page(), page.size(), total);
    }

    private void appendFilters(StringBuilder query, List<Object> parameters, Boolean completed, UUID groupId,
            UUID folderId) {
        if (completed != null) {
            query.append(" and t.completed = ?").append(parameters.size() + 1);
            parameters.add(completed);
        }
        if (groupId != null) {
            query.append(" and t.sharedGroup.id = ?").append(parameters.size() + 1);
            parameters.add(groupId);
        }
        if (folderId != null) {
            query.append(" and t.folder.id = ?").append(parameters.size() + 1);
            parameters.add(folderId);
        }
    }
}
