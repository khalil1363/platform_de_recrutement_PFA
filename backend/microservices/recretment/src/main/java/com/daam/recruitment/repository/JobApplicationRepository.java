package com.daam.recruitment.repository;

import com.daam.recruitment.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    Optional<JobApplication> findByApplicationId(String applicationId);
    List<JobApplication> findByRecruitmentId(String recruitmentId);
    List<JobApplication> findByRecruitmentIdIn(List<String> recruitmentIds);
    boolean existsByRecruitmentIdAndCandidateUserId(String recruitmentId, String candidateUserId);
    List<JobApplication> findByCandidateUserId(String candidateUserId);
}
