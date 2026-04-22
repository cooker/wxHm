package com.wxhm.repository;

import com.wxhm.entity.GroupAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GroupAliasRepository extends JpaRepository<GroupAlias, Long> {
    Optional<GroupAlias> findByGroupName(String groupName);
    Optional<GroupAlias> findByShortName(String shortName);
    List<GroupAlias> findAllByGroupNameIn(Collection<String> groupNames);
    void deleteByGroupName(String groupName);
}
