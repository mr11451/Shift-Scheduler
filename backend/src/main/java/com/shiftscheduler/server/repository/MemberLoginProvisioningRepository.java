package com.shiftscheduler.server.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.shiftscheduler.server.domain.MemberLoginProvisioning;
import com.shiftscheduler.server.domain.MemberLoginProvisioningStatus;

@Repository
public interface MemberLoginProvisioningRepository extends JpaRepository<MemberLoginProvisioning, Long> {
    Optional<MemberLoginProvisioning> findByStaffId(Long staffId);

    Optional<MemberLoginProvisioning> findByLoginCode(String loginCode);

    @Query("SELECT mlp FROM MemberLoginProvisioning mlp WHERE mlp.staff.id = :staffId AND mlp.status = :status")
    Optional<MemberLoginProvisioning> findByStaffIdAndStatus(@Param("staffId") Long staffId, @Param("status") MemberLoginProvisioningStatus status);

    List<MemberLoginProvisioning> findByStatus(MemberLoginProvisioningStatus status);
}
