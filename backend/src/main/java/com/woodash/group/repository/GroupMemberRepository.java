package com.woodash.group.repository;

import com.woodash.group.entity.GroupMember;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
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

    public List<GroupMember> listForGroup(UUID groupId) {
        return list("sharedGroup.id = ?1 order by user.login", groupId);
    }
}
