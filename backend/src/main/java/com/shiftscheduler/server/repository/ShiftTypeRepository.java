package com.shiftscheduler.server.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.shiftscheduler.server.domain.ShiftType;

@Repository
public interface ShiftTypeRepository extends JpaRepository<ShiftType, Long> {
    Optional<ShiftType> findByShiftCode(String shiftCode);

    List<ShiftType> findAllByIsActiveTrue();

    @Query("SELECT s FROM ShiftType s WHERE s.isActive = true ORDER BY s.sortOrder ASC, s.shiftCode ASC")
    List<ShiftType> findAllActive();

    @Query("SELECT s FROM ShiftType s WHERE s.isActive = true AND s.isOffType = false ORDER BY s.sortOrder ASC, s.shiftCode ASC")
    List<ShiftType> findAllActiveWorkShifts();
}
