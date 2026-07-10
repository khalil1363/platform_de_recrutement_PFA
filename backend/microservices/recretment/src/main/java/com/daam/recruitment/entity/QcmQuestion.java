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

    @PrePersist void prePersist() {
        if (questionId == null) questionId = UUID.randomUUID().toString();
    }
}
