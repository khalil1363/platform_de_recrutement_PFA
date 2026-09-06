package com.daam.reclamation.repository;

import com.daam.reclamation.entity.Reclamation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {
    Optional<Reclamation> findByReclamationId(String reclamationId);

    List<Reclamation> findByCreatedByUserIdOrderByCreatedAtDesc(String createdByUserId);

    List<Reclamation> findAllByOrderByCreatedAtDesc();
}
