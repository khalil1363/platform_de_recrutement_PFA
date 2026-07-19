package com.daam.recruitment.repository;

import com.daam.recruitment.entity.HiredQcmDimensionScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HiredQcmDimensionScoreRepository extends JpaRepository<HiredQcmDimensionScore, Long> {
    List<HiredQcmDimensionScore> findByAssignmentIdOrderBySortOrderAsc(String assignmentId);

    void deleteByAssignmentId(String assignmentId);

    List<HiredQcmDimensionScore> findByAssignmentIdInOrderByAssignmentIdAscSortOrderAsc(List<String> assignmentIds);
}
