package com.daam.recruitment.entity;

import com.daam.recruitment.enumeration.InterviewStatus;
import com.daam.recruitment.enumeration.MeetingProviderType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Interview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String interviewId;

    @Column(nullable = false)
    private String applicationId;

    @Column(nullable = false)
    private String recruitmentId;

    @Column(nullable = false)
    private String recruiterUserId;

    @Column(nullable = false)
    private String candidateUserId;

    @Column(nullable = false, length = 512)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MeetingProviderType meetingProvider = MeetingProviderType.NONE;

    private String meetingId;

    @Column(length = 1024)
    private String meetingLink;

    @Column(columnDefinition = "TEXT")
    private String meetingWarning;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (interviewId == null) {
            interviewId = UUID.randomUUID().toString();
        }
    }
}
