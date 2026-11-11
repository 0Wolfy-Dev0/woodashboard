package com.woodash.group.repository;

import com.woodash.common.PageRequest;
import com.woodash.common.PageResponse;
import com.woodash.group.entity.GroupMember;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class GroupMemberRepository implements PanacheRepositoryBase<GroupMember, UUID> {

    public boolean existsFor(UUID groupId, UUID userId) {
        return count("sharedGroup.id = ?1 and user.id = ?2", groupId, userId) > 0;
    }

    public Optional<GroupMember> findFor(UUID groupId, UUID userId) {
        return find("sharedGroup.id = ?1 and user.id = ?2", groupId, userId).firstResultOptional();
    }

    public PageResponse<GroupMember> listForGroupPage(UUID groupId, PageRequest page) {
        long total = count("sharedGroup.id = ?1", groupId);
        var paged = find("select gm from GroupMember gm join fetch gm.user"
                + " where gm.sharedGroup.id = ?1 order by gm.user.login", groupId);
        return PageResponse.of(paged.page(page.page(), page.size()).list(), page.page(), page.size(), total);
    }
}
