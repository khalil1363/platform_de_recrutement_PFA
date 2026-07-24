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
        private Double latitude;
        private Double longitude;
        private String googleMapsUrl;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CompanyResponse {
        private String companyId;
        private String name;
        private String zoneId;
        private String zoneName;
        private String address;
        private Double latitude;
        private Double longitude;
        private String googleMapsUrl;
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
        private String qcmId;
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
        private String qcmId;
        private String qcmTitle;
        private List<QcmQuestionResponse> questions;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class QcmRequest {
        @NotBlank private String title;
        private String description;
        @NotNull private List<QcmQuestionRequest> questions;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class QcmResponse {
        private String qcmId;
        private String title;
        private String description;
        private String createdByRhUserId;
        private LocalDateTime createdAt;
        private int questionCount;
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
        private String dimensionCode;
        private Double scoreA;
        private Double scoreB;
        private Double scoreC;
        private Double scoreD;
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
        private String dimensionCode;
        private Double scoreA;
        private Double scoreB;
        private Double scoreC;
        private Double scoreD;
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
        private LocalDateTime interviewEndAt;
        private Integer durationMinutes;
        /** ONLINE (default) or PHYSICAL */
        private String interviewType;
        /** Optional override for physical interview location */
        private String interviewLocation;
        /** Hire confirmation fields (when status = HIRED) */
        private java.time.LocalDate hireStartDate;
        private String hireContractType;
        private String hireNetSalary;
        private String hireWorkingHours;
        private String hireBenefits;
        private String hireIntegrationAddress;
        private String hireIntegrationGpsUrl;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ApplicationRequest {
        @NotBlank private String recruitmentId;
        private String cvFileUrl;
        private List<QcmAnswerRequest> answers;
        /** When true (anti-cheat triggered), QCM score is forced to 0. */
        private Boolean qcmViolated;
    }

    /** RH can complete Excel suivi fields missing from auto application data. */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ApplicationTrackingUpdateRequest {
        private String provenance;
        private String imf;
        private String profilMetier;
        private String experienceYears;
        private String situationPerso;
        private String salaireActuel;
        private String disponibilite;
        private String commentairesRh;
        private String contactName;
        private String commercialName;
        private String codeDossier;
        private String diplomeEcole;
        private String formatMission;
        private java.time.LocalDate dateDebutMission;
        private java.time.LocalDate dateFinMission;
        private String dureeContrat;
        private String prixMois;
        private String competence;
        private String aNegocier;
        private String deplacement;
        private String pretention;
        private String remarquesRh;
        private String affectation;
        private String desistement;
        private String composante;
        private String observation;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ApplicationResponse {
        private String applicationId;
        private String recruitmentId;
        private String recruitmentTitle;
        private String zoneName;
        private String region;
        private String city;
        private String candidateUserId;
        private UserSummary candidate;
        private String cvFileUrl;
        private ApplicationStatus status;
        private Integer qcmScore;
        private Integer qcmTotalQuestions;
        private Integer cvMatchScore;
        private String extractedSkills;
        private String matchedSkills;
        private String missingSkills;
        private String cvAnalysisSummary;
        private LocalDateTime cvAnalyzedAt;
        private LocalDateTime interviewAt;
        private LocalDateTime interviewEndAt;
        private String googleMeetLink;
        private String meetingProvider;
        private String meetingId;
        private String meetingWarning;
        private String companyName;
        private String companyAddress;
        private String companyGoogleMapsUrl;
        private LocalDateTime hiredAt;
        private java.time.LocalDate hireStartDate;
        private String hireContractType;
        private String hireNetSalary;
        private String hireWorkingHours;
        private String hireBenefits;
        private String hireIntegrationAddress;
        private String hireIntegrationGpsUrl;
        private String provenance;
        private String imf;
        private String profilMetier;
        private String experienceYears;
        private String situationPerso;
        private String salaireActuel;
        private String disponibilite;
        private String commentairesRh;
        private String contactName;
        private String commercialName;
        private String codeDossier;
        private String diplomeEcole;
        private String formatMission;
        private java.time.LocalDate dateDebutMission;
        private java.time.LocalDate dateFinMission;
        private String dureeContrat;
        private String prixMois;
        private String competence;
        private String aNegocier;
        private String deplacement;
        private String pretention;
        private String remarquesRh;
        private String affectation;
        private String desistement;
        private String composante;
        private String observation;
        private String keejobReference;
        private String internalReference;
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

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AssignHiredQcmRequest {
        @NotBlank private String qcmId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class HiredQcmSubmitRequest {
        private List<QcmAnswerRequest> answers;
        private Boolean qcmViolated;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class HiredQcmAssignmentResponse {
        private String assignmentId;
        private String applicationId;
        private String candidateUserId;
        private String recruitmentId;
        private String recruitmentTitle;
        private String companyName;
        private String qcmId;
        private String qcmTitle;
        private String status;
        private Integer score;
        private Integer totalQuestions;
        private Integer overallFitPercent;
        private Boolean qcmViolated;
        private LocalDateTime assignedAt;
        private LocalDateTime completedAt;
        private UserSummary candidate;
        private List<QcmQuestionResponse> questions;
        private List<ApplicationAnswerResponse> answers;
        private List<DimensionScoreResponse> dimensionScores;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DimensionScoreResponse {
        private String dimensionCode;
        private String dimensionLabel;
        private Double score;
        private Double expectedScore;
        private String commentText;
        private Integer sortOrder;
    }
}
