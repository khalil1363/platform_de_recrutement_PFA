package com.daam.recruitment.repository;

import com.daam.recruitment.entity.QcmQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface QcmQuestionRepository extends JpaRepository<QcmQuestion, Long> {
    List<QcmQuestion> findByQcmIdOrderByOrderIndexAsc(String qcmId);
    Optional<QcmQuestion> findByQuestionId(String questionId);

    @Modifying
    @Query("DELETE FROM QcmQuestion q WHERE q.qcmId = :qcmId")
    void deleteByQcmId(String qcmId);

    long countByQcmId(String qcmId);
}
