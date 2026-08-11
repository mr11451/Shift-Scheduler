package com.shiftscheduler.server.repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.shiftscheduler.server.domain.ShiftAssignment;

@Repository
public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {
    Optional<ShiftAssignment> findByStaffIdAndWorkDate(Long staffId, LocalDate workDate);

    List<ShiftAssignment> findByStaffIdOrderByWorkDateAsc(Long staffId);

    @Query("SELECT sa FROM ShiftAssignment sa WHERE sa.staff.id = :staffId AND sa.workDate BETWEEN :startDate AND :endDate ORDER BY sa.workDate ASC")
    List<ShiftAssignment> findByStaffIdAndDateRange(@Param("staffId") Long staffId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT sa FROM ShiftAssignment sa WHERE sa.staff.group.id = :groupId AND sa.workDate BETWEEN :startDate AND :endDate ORDER BY sa.staff.staffCode ASC, sa.workDate ASC")
    List<ShiftAssignment> findByGroupIdAndDateRange(@Param("groupId") Long groupId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT sa FROM ShiftAssignment sa WHERE sa.workDate BETWEEN :startDate AND :endDate ORDER BY sa.staff.staffCode ASC, sa.workDate ASC")
    List<ShiftAssignment> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<ShiftAssignment> findByStaffIdInAndWorkDateBetween(List<Long> staffIds, LocalDate startDate, LocalDate endDate);

    void deleteByStaffIdInAndWorkDateBetween(List<Long> staffIds, LocalDate startDate, LocalDate endDate);

    @Query("DELETE FROM ShiftAssignment sa WHERE sa.staff.id = :staffId AND sa.workDate BETWEEN :startDate AND :endDate")
    void deleteByStaffIdAndDateRange(@Param("staffId") Long staffId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
