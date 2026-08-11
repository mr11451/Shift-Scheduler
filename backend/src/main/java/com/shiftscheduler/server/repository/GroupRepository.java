package com.shiftscheduler.server.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.shiftscheduler.server.domain.Group;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findByGroupCode(String groupCode);

    List<Group> findAllByIsActiveTrue();

    @Query("SELECT g FROM Group g WHERE g.isActive = true ORDER BY g.groupCode ASC")
    List<Group> findAllActiveGroups();
}
