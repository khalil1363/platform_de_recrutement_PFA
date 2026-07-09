package com.daam.recruitment.dto;

import com.daam.recruitment.enumeration.ApplicationStatus;
import com.daam.recruitment.enumeration.RecruitmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class RecruitmentDtos {

    private RecruitmentDtos() {}

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ZoneRequest {
        @NotBlank private String name;
        private String description;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ZoneResponse {
        private String zoneId;
        private String name;
        private String description;
        private boolean active;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CompanyRequest {
        @NotBlank private String name;
        @NotBlank private String zoneId;
        private String address;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CompanyResponse {
        private String companyId;
        private String name;
        private String zoneId;
        private String zoneName;
        private String address;
        private boolean active;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RhZoneAssignmentRequest {
        @NotBlank private String rhUserId;
        @NotNull private List<String> zoneIds;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RhZoneAssignmentResponse {
        private Long id;
        private String rhUserId;
        private String zoneId;
        private String zoneName;
        private LocalDateTime assignedAt;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RecruitmentRequest {
        @NotBlank private String title;
        private String description;
        private String responsibilities;
        private String technicalSkills;
        private String personalSkills;
        private String educationRequirements;
        private String experienceRequirements;
        @NotBlank private String companyId;
        private String jobType;
        private String availability;
        private BigDecimal salaryMin;
        private BigDecimal salaryMax;
        private String salaryPeriod;
        private List<String> languages;
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
        private RecruitmentStatus status;
        private List<QcmQuestionRequest> questions;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RecruitmentResponse {
        private String recruitmentId;
        private String title;
        private String description;
        private String responsibilities;
        private String technicalSkills;
        private String personalSkills;
        private String educationRequirements;
        private String experienceRequirements;
        private String companyId;
        private String companyName;
        private String zoneId;
        private String zoneName;
        private String jobType;
        private String availability;
        private BigDecimal salaryMin;
        private BigDecimal salaryMax;
        private String salaryPeriod;
        private List<String> languages;
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
        private String internalReference;
        private String keejobReference;
        private RecruitmentStatus status;
        private LocalDateTime createdAt;
        private List<QcmQuestionResponse> questions;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class QcmQuestionRequest {
        @NotBlank private String questionText;
        @NotBlank private String optionA;
        @NotBlank private String optionB;
        private String optionC;
        private String optionD;
        @NotBlank private String correctOption;
        private Integer orderIndex;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class QcmQuestionResponse {
        private String questionId;
        private String questionText;
        private String optionA;
        private String optionB;
        private String optionC;
        private String optionD;
        private String correctOption;
        private Integer orderIndex;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class QcmAnswerRequest {
        @NotBlank private String questionId;
        @NotBlank private String selectedOption;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ApplicationStatusUpdateRequest {
        @NotNull private ApplicationStatus status;
        private LocalDateTime interviewAt;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ApplicationRequest {
        @NotBlank private String recruitmentId;
        private String cvFileUrl;
        private List<QcmAnswerRequest> answers;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ApplicationResponse {
        private String applicationId;
        private String recruitmentId;
        private String recruitmentTitle;
        private String zoneName;
        private String region;
        private String candidateUserId;
        private UserSummary candidate;
        private String cvFileUrl;
        private ApplicationStatus status;
        private Integer qcmScore;
        private Integer qcmTotalQuestions;
        private LocalDateTime interviewAt;
        private LocalDateTime appliedAt;
        private List<ApplicationAnswerResponse> answers;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ApplicationAnswerResponse {
        private String questionId;
        private String questionText;
        private String selectedOption;
        private String correctOption;
        private boolean correct;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UserSummary {
        private String userId;
        private String firstName;
        private String lastName;
        private String username;
        private String email;
        private String phoneNumber;
        private String profileImageUrl;
    }
}
