package com.daam.recruitment.repository;

import com.daam.recruitment.entity.ApplicationAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApplicationAnswerRepository extends JpaRepository<ApplicationAnswer, Long> {
    List<ApplicationAnswer> findByApplicationId(String applicationId);
    void deleteByApplicationId(String applicationId);
}
