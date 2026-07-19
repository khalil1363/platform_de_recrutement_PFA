package com.daam.recruitment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hired_qcm_answers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HiredQcmAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String assignmentId;

    @Column(nullable = false)
    private String questionId;

    @Column(nullable = false)
    private String selectedOption;

    private boolean correct;
}
