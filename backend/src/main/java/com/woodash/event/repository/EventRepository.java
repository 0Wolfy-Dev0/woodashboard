package com.woodash.event.repository;

import com.woodash.event.entity.Event;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
                "select distinct e from Event e left join e.sharedGroup g left join g.members gm"
                        + " where ((e.sharedGroup is null and e.user.id = ?1)"
                        + " or (e.sharedGroup is not null and gm.user.id = ?1))");
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);
        appendWindow(query, parameters, from, to);
        query.append(" order by e.startTime");
        return list(query.toString(), parameters.toArray());
    }

    public List<Event> listByGroup(UUID groupId, LocalDateTime from, LocalDateTime to) {
        StringBuilder query = new StringBuilder("from Event e where e.sharedGroup.id = ?1");
        List<Object> parameters = new ArrayList<>();
        parameters.add(groupId);
        appendWindow(query, parameters, from, to);
        query.append(" order by e.startTime");
        return list(query.toString(), parameters.toArray());
    }

    public List<Event> listAllOrdered() {
        return list("order by startTime");
    }

    private void appendWindow(StringBuilder query, List<Object> parameters, LocalDateTime from, LocalDateTime to) {
        if (from != null) {
            query.append(" and e.endTime >= ?").append(parameters.size() + 1);
            parameters.add(from);
        }
        if (to != null) {
            query.append(" and e.startTime <= ?").append(parameters.size() + 1);
            parameters.add(to);
        }
    }
}
