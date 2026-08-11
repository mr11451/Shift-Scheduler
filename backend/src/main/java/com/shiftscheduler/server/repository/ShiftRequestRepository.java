package com.shiftscheduler.server.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.shiftscheduler.server.domain.ShiftRequest;
import com.shiftscheduler.server.domain.ShiftRequestStatus;

@Repository
public interface ShiftRequestRepository extends JpaRepository<ShiftRequest, Long> {
    Optional<ShiftRequest> findByStaffIdAndWorkDate(Long staffId, LocalDate workDate);

    List<ShiftRequest> findByStaffIdOrderByWorkDateAsc(Long staffId);

    List<ShiftRequest> findByStaffIdAndStatus(Long staffId, ShiftRequestStatus status);

    @Query("SELECT sr FROM ShiftRequest sr WHERE sr.staff.id = :staffId AND sr.workDate BETWEEN :startDate AND :endDate ORDER BY sr.workDate ASC")
    List<ShiftRequest> findByStaffIdAndDateRange(@Param("staffId") Long staffId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<ShiftRequest> findByStaffIdInAndWorkDateBetween(List<Long> staffIds, LocalDate startDate, LocalDate endDate);

    @Query("SELECT sr FROM ShiftRequest sr WHERE sr.staff.id = :staffId AND sr.workDate BETWEEN :startDate AND :endDate AND sr.status = :status ORDER BY sr.workDate ASC")
    List<ShiftRequest> findByStaffIdAndDateRangeAndStatus(@Param("staffId") Long staffId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("status") ShiftRequestStatus status);

    @Query("SELECT sr FROM ShiftRequest sr WHERE sr.staff.group.id = :groupId AND sr.workDate BETWEEN :startDate AND :endDate ORDER BY sr.staff.staffCode ASC, sr.workDate ASC")
    List<ShiftRequest> findByGroupIdAndDateRange(@Param("groupId") Long groupId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT sr FROM ShiftRequest sr WHERE sr.workDate BETWEEN :startDate AND :endDate AND sr.status = :status ORDER BY sr.staff.staffCode ASC, sr.workDate ASC")
    List<ShiftRequest> findUnreflectedByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("status") ShiftRequestStatus status);
}
