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
    private LocalDateTime interviewAt;
    @CreationTimestamp private LocalDateTime appliedAt;

    @PrePersist void prePersist() {
        if (applicationId == null) applicationId = UUID.randomUUID().toString();
    }
}
