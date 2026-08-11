package com.shiftscheduler.server.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.shiftscheduler.server.domain.RoleLevel;
import com.shiftscheduler.server.domain.Staff;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByStaffCode(String staffCode);

    Optional<Staff> findByEmail(String email);

    List<Staff> findAllByIsActiveTrue();

    List<Staff> findAllByRoleLevel(RoleLevel roleLevel);

    @Query("SELECT s FROM Staff s WHERE s.isActive = true AND s.roleLevel = ?1 ORDER BY s.staffCode ASC")
    List<Staff> findAllActiveByRoleLevel(RoleLevel roleLevel);

    @Query("SELECT s FROM Staff s WHERE s.isActive = true AND s.group.id = ?1 ORDER BY s.staffCode ASC, s.staffName ASC")
    List<Staff> findAllActiveByGroupId(Long groupId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Staff s WHERE s.staffCode = :staffCode AND s.id != :excludeId")
    boolean existsStaffCodeExcluding(@Param("staffCode") String staffCode, @Param("excludeId") Long excludeId);
}
