package com.daam.recruitment.repository;

import com.daam.recruitment.entity.Interview;
import com.daam.recruitment.enumeration.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    Optional<Interview> findByApplicationIdAndStatus(String applicationId, InterviewStatus status);
}
