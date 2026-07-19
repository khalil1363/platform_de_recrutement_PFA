package com.daam.recruitment.repository;

import com.daam.recruitment.entity.HiredQcmAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HiredQcmAnswerRepository extends JpaRepository<HiredQcmAnswer, Long> {
    List<HiredQcmAnswer> findByAssignmentId(String assignmentId);

    void deleteByAssignmentId(String assignmentId);
}
