package com.shiftscheduler.server.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.shiftscheduler.server.domain.CalendarViewPermission;
import com.shiftscheduler.server.domain.CalendarViewPermissionStatus;

@Repository
public interface CalendarViewPermissionRepository extends JpaRepository<CalendarViewPermission, Long> {
    @Query("SELECT cvp FROM CalendarViewPermission cvp WHERE cvp.requesterStaff.id = :requesterId AND cvp.targetStaff.id = :targetId AND cvp.status = :status")
    Optional<CalendarViewPermission> findApprovedPermission(@Param("requesterId") Long requesterId, @Param("targetId") Long targetId, @Param("status") CalendarViewPermissionStatus status);

    List<CalendarViewPermission> findByRequesterStaffIdOrderByRequestedAtDesc(Long requesterStaffId);

    List<CalendarViewPermission> findByTargetStaffIdAndStatusOrderByRequestedAtDesc(Long targetStaffId, CalendarViewPermissionStatus status);

    @Query("SELECT cvp FROM CalendarViewPermission cvp WHERE cvp.requesterStaff.id = :requesterId AND cvp.status = :status ORDER BY cvp.requestedAt DESC")
    List<CalendarViewPermission> findByRequesterAndStatus(@Param("requesterId") Long requesterId, @Param("status") CalendarViewPermissionStatus status);

    @Query("SELECT cvp.targetStaff.id FROM CalendarViewPermission cvp WHERE cvp.requesterStaff.id = :requesterId AND cvp.status = :status")
    List<Long> findApprovedTargetStaffIds(@Param("requesterId") Long requesterId, @Param("status") CalendarViewPermissionStatus status);

    List<CalendarViewPermission> findByTargetStaffIdOrderByRequestedAtDesc(Long targetStaffId);

    @Query("SELECT cvp FROM CalendarViewPermission cvp WHERE cvp.targetStaff.id = :targetStaffId AND cvp.status = :status ORDER BY cvp.requestedAt DESC")
    List<CalendarViewPermission> findByTargetStaffIdAndStatus(@Param("targetStaffId") Long targetStaffId, @Param("status") CalendarViewPermissionStatus status);
}
