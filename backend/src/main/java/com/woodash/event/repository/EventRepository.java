package com.woodash.event.repository;

import com.woodash.common.PageRequest;
import com.woodash.common.PageResponse;
import com.woodash.event.entity.Event;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class EventRepository implements PanacheRepositoryBase<Event, UUID> {

    public List<Event> listByUser(UUID userId) {
        return list("user.id = ?1 order by startTime", userId);
    }

    /** Events of a user overlapping the [from, to] window. */
    public List<Event> listByUserBetween(UUID userId, LocalDateTime from, LocalDateTime to) {
        return list("user.id = ?1 and endTime >= ?2 and startTime <= ?3 order by startTime", userId, from, to);
    }

    public List<Event> listByUserFrom(UUID userId, LocalDateTime from) {
        return list("user.id = ?1 and endTime >= ?2 order by startTime", userId, from);
    }

    public List<Event> listByUserTo(UUID userId, LocalDateTime to) {
        return list("user.id = ?1 and startTime <= ?2 order by startTime", userId, to);
    }

    public List<Event> listAccessible(UUID userId, LocalDateTime from, LocalDateTime to) {
        StringBuilder query = new StringBuilder(
                "select distinct e from Event e left join fetch e.user"
                        + " left join e.sharedGroup g left join g.members gm"
                        + " where ((e.sharedGroup is null and e.user.id = ?1)"
                        + " or (e.sharedGroup is not null and gm.user.id = ?1))");
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);
        appendWindow(query, parameters, from, to);
        query.append(" order by e.startTime");
        return list(query.toString(), parameters.toArray());
    }

    public List<Event> listByGroup(UUID groupId, LocalDateTime from, LocalDateTime to) {
        StringBuilder query = new StringBuilder(
                "select e from Event e left join fetch e.user where e.sharedGroup.id = ?1");
        List<Object> parameters = new ArrayList<>();
        parameters.add(groupId);
        appendWindow(query, parameters, from, to);
        query.append(" order by e.startTime");
        return list(query.toString(), parameters.toArray());
    }

    public PageResponse<Event> searchAllPage(String search, LocalDateTime from, LocalDateTime to,
            PageRequest page) {
        StringBuilder query = new StringBuilder(
                "select e from Event e join fetch e.user u where 1 = 1");
        List<Object> parameters = new ArrayList<>();
        if (search != null) {
            String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
            int first = parameters.size() + 1;
            query.append(" and (lower(e.name) like ?").append(first)
                    .append(" or lower(coalesce(e.description, '')) like ?").append(first + 1)
                    .append(" or lower(u.login) like ?").append(first + 2).append(")");
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
        }
        if (from != null) {
            query.append(" and e.endTime >= ?").append(parameters.size() + 1);
            parameters.add(from);
        }
        if (to != null) {
            query.append(" and e.startTime <= ?").append(parameters.size() + 1);
            parameters.add(to);
        }
        query.append(" order by e.startTime");
        PanacheQuery<Event> paged = find(query.toString(), parameters.toArray());
        long total = paged.count();
        return PageResponse.of(paged.page(page.page(), page.size()).list(), page.page(), page.size(), total);
    }

    private void appendWindow(StringBuilder query, List<Object> parameters, LocalDateTime from, LocalDateTime to) {
        if (from != null) {
            // Recurring series may start before the requested window, so keep them when
            // their series end still reaches the window. Non-recurring rows use the
            // ordinary overlap check.
            int seriesEndParameter = parameters.size() + 1;
            query.append(" and ((e.recurrenceUnit is not null and (e.recurrenceEnd is null"
                    + " or e.recurrenceEnd >= ?").append(seriesEndParameter)
                    .append(")) or e.endTime >= ?").append(seriesEndParameter + 1).append(")");
            parameters.add(from);
            parameters.add(from);
        }
        if (to != null) {
            query.append(" and e.startTime <= ?").append(parameters.size() + 1);
            parameters.add(to);
        }
    }
}
