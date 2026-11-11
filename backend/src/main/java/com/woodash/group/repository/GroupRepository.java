package com.woodash.group.repository;

import com.woodash.common.PageRequest;
import com.woodash.common.PageResponse;
import com.woodash.group.entity.Group;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class GroupRepository implements PanacheRepositoryBase<Group, UUID> {

    public PageResponse<Group> listForUserPage(UUID userId, PageRequest page) {
        PanacheQuery<Group> paged = find(
                "select distinct g from Group g join g.members m where m.user.id = ?1 order by g.name",
                userId);
        long total = paged.count();
        return PageResponse.of(paged.page(page.page(), page.size()).list(), page.page(), page.size(), total);
    }
}
