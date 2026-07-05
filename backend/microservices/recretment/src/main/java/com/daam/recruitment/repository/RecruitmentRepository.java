package com.daam.recruitment.repository;

import com.daam.recruitment.entity.Recruitment;
import com.daam.recruitment.enumeration.RecruitmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RecruitmentRepository extends JpaRepository<Recruitment, Long> {
    Optional<Recruitment> findByRecruitmentId(String recruitmentId);
    List<Recruitment> findByZoneId(String zoneId);
    List<Recruitment> findByZoneIdAndStatus(String zoneId, RecruitmentStatus status);
    List<Recruitment> findByStatus(RecruitmentStatus status);
    List<Recruitment> findByCompanyId(String companyId);
}
