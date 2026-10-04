package com.woodash.event.repository;

import com.woodash.event.entity.Event;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
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
}
