package com.daam.recruitment.repository;

import com.daam.recruitment.entity.RhZoneAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RhZoneAssignmentRepository extends JpaRepository<RhZoneAssignment, Long> {
    List<RhZoneAssignment> findByRhUserId(String rhUserId);
    Optional<RhZoneAssignment> findByRhUserIdAndZoneId(String rhUserId, String zoneId);
    Optional<RhZoneAssignment> findByZoneId(String zoneId);
    boolean existsByRhUserIdAndZoneId(String rhUserId, String zoneId);
}
