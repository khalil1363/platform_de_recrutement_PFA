package com.daam.recruitment.entity;

import com.daam.recruitment.enumeration.RecruitmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "recruitments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Recruitment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, updatable = false)
    private String recruitmentId;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(columnDefinition = "TEXT") private String responsibilities;
    @Column(columnDefinition = "TEXT") private String technicalSkills;
    @Column(columnDefinition = "TEXT") private String personalSkills;
    @Column(columnDefinition = "TEXT") private String educationRequirements;
    @Column(columnDefinition = "TEXT") private String experienceRequirements;
    @Column(nullable = false) private String companyId;
    @Column(nullable = false) private String zoneId;
    private String jobType;
    private String availability;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String salaryPeriod;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recruitment_languages", joinColumns = @JoinColumn(name = "recruitment_id"))
    @Column(name = "language")
    @Builder.Default private List<String> languages = new ArrayList<>();
    private String educationLevel;
    private String experienceLevel;
    private Boolean drivingLicenseRequired;
    private String country;
    private String region;
    private String city;
    private Boolean localTravel;
    private Boolean internationalTravel;
    private Boolean anonymousMode;
    private LocalDate publicationDate;
    private String responsibleName;
    private Boolean emailNotificationPerApplication;
    private String internalReference;
    private String keejobReference;
    @Enumerated(EnumType.STRING)
    @Builder.Default private RecruitmentStatus status = RecruitmentStatus.DRAFT;
    @Column(nullable = false) private String createdByRhUserId;
    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;

    @PrePersist void prePersist() {
        if (recruitmentId == null) recruitmentId = UUID.randomUUID().toString();
    }
}
