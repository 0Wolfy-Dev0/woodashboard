package com.woodash.group.repository;

import com.woodash.group.entity.Group;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class GroupRepository implements PanacheRepositoryBase<Group, UUID> {

    public List<Group> listForUser(UUID userId) {
        return find("select distinct g from Group g join g.members m where m.user.id = ?1 order by g.name",
                userId).list();
    }
}
