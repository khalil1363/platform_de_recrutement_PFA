package com.daam.recruitment.entity;

import com.daam.recruitment.enumeration.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_applications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, updatable = false)
    private String applicationId;
    @Column(nullable = false) private String recruitmentId;
    @Column(nullable = false) private String candidateUserId;
    private String cvFileUrl;
    @Enumerated(EnumType.STRING)
    @Builder.Default private ApplicationStatus status = ApplicationStatus.SUBMITTED;
    private Integer qcmScore;
    private Integer qcmTotalQuestions;
    /** 0–100 match between CV skills and job requirements */
    private Integer cvMatchScore;
    @Column(columnDefinition = "TEXT")
    private String extractedSkills;
    @Column(columnDefinition = "TEXT")
    private String matchedSkills;
    @Column(columnDefinition = "TEXT")
    private String missingSkills;
    @Column(columnDefinition = "TEXT")
    private String cvAnalysisSummary;
    private LocalDateTime cvAnalyzedAt;
    private LocalDateTime interviewAt;
    @Column(length = 512)
    private String googleMeetLink;
    @CreationTimestamp private LocalDateTime appliedAt;

    @PrePersist void prePersist() {
        if (applicationId == null) applicationId = UUID.randomUUID().toString();
    }
}
