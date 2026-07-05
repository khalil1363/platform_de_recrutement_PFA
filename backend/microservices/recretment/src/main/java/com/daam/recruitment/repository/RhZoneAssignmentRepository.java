package com.daam.recruitment.repository;

import com.daam.recruitment.entity.RhZoneAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RhZoneAssignmentRepository extends JpaRepository<RhZoneAssignment, Long> {
    Optional<RhZoneAssignment> findByRhUserId(String rhUserId);
    boolean existsByRhUserId(String rhUserId);
}
