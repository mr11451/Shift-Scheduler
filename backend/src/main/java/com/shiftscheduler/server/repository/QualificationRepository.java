package com.shiftscheduler.server.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.shiftscheduler.server.domain.Qualification;

@Repository
public interface QualificationRepository extends JpaRepository<Qualification, Long> {
    Optional<Qualification> findByQualificationName(String qualificationName);

    List<Qualification> findAllByIsActiveTrue();

    @Query("SELECT q FROM Qualification q WHERE q.isActive = true ORDER BY q.qualificationName ASC")
    List<Qualification> findAllActive();
}
