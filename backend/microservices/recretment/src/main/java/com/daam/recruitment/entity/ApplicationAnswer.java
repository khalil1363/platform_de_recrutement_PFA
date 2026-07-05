package com.daam.recruitment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "application_answers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApplicationAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String applicationId;
    @Column(nullable = false) private String questionId;
    @Column(nullable = false) private String selectedOption;
    private boolean correct;
}
