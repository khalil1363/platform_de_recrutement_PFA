package com.daam.recruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "qcm_questions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QcmQuestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, updatable = false)
    private String questionId;
    @Column(name = "qcm_id")
    private String qcmId;
    @Column(nullable = false, columnDefinition = "TEXT") private String questionText;
    @Column(nullable = false) private String optionA;
    @Column(nullable = false) private String optionB;
    private String optionC;
    private String optionD;
    @Column(nullable = false) private String correctOption;
    private Integer orderIndex;

    /** Psychometric competency code (e.g. APPROCHE_CLIENT). Optional for classic QCMs. */
    @Column(name = "dimension_code")
    private String dimensionCode;

    /** Option weights 0..10 used for psychometric scoring. */
    @Column(name = "score_a")
    private Double scoreA;
    @Column(name = "score_b")
    private Double scoreB;
    @Column(name = "score_c")
    private Double scoreC;
    @Column(name = "score_d")
    private Double scoreD;

    @PrePersist void prePersist() {
        if (questionId == null) questionId = UUID.randomUUID().toString();
    }
}
