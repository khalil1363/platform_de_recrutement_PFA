package com.daam.recruitment.repository;

import com.daam.recruitment.entity.Qcm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QcmRepository extends JpaRepository<Qcm, Long> {
    Optional<Qcm> findByQcmId(String qcmId);
    List<Qcm> findAllByOrderByCreatedAtDesc();
    List<Qcm> findByCreatedByRhUserIdOrderByCreatedAtDesc(String createdByRhUserId);
    boolean existsByQcmId(String qcmId);
}
