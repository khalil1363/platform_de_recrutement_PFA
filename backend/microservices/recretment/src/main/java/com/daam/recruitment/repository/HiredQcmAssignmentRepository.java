package com.daam.recruitment.repository;

import com.daam.recruitment.entity.HiredQcmAssignment;
import com.daam.recruitment.enumeration.HiredQcmStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HiredQcmAssignmentRepository extends JpaRepository<HiredQcmAssignment, Long> {
    Optional<HiredQcmAssignment> findByAssignmentId(String assignmentId);

    List<HiredQcmAssignment> findByCandidateUserIdOrderByAssignedAtDesc(String candidateUserId);

    List<HiredQcmAssignment> findByApplicationIdOrderByAssignedAtDesc(String applicationId);

    boolean existsByApplicationIdAndQcmIdAndStatus(String applicationId, String qcmId, HiredQcmStatus status);

    List<HiredQcmAssignment> findByStatusInOrderByCompletedAtDesc(List<HiredQcmStatus> statuses);
}
