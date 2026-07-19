package com.daam.recruitment.entity;

import com.daam.recruitment.enumeration.HiredQcmStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hired_qcm_assignments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HiredQcmAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String assignmentId;

    @Column(nullable = false)
    private String applicationId;

    @Column(nullable = false)
    private String candidateUserId;

    @Column(nullable = false)
    private String recruitmentId;

    @Column(nullable = false)
    private String qcmId;

    @Column(nullable = false)
    private String assignedByRhUserId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private HiredQcmStatus status = HiredQcmStatus.ASSIGNED;

    private Integer score;
    private Integer totalQuestions;
    /** Overall job-fit percentage 0..100 when psychometric dimensions are scored. */
    private Integer overallFitPercent;
    private Boolean qcmViolated;
    private LocalDateTime completedAt;

    @CreationTimestamp
    private LocalDateTime assignedAt;

    @PrePersist
    void prePersist() {
        if (assignmentId == null) {
            assignmentId = UUID.randomUUID().toString();
        }
    }
}
