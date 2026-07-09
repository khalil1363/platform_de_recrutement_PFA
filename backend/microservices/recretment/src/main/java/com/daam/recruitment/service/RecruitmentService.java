package com.daam.recruitment.service;

import com.daam.recruitment.client.UserClient;
import com.daam.recruitment.client.UserDto;
import com.daam.recruitment.dto.RecruitmentDtos.*;
import com.daam.recruitment.entity.*;
import com.daam.recruitment.enumeration.ApplicationStatus;
import com.daam.recruitment.enumeration.RecruitmentStatus;
import com.daam.recruitment.repository.*;
import com.daam.recruitment.response.ApiResponse;
import com.daam.recruitment.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecruitmentService {

    private final ZoneRepository zoneRepository;
    private final CompanyRepository companyRepository;
    private final RhZoneAssignmentRepository rhZoneAssignmentRepository;
    private final RecruitmentRepository recruitmentRepository;
    private final QcmQuestionRepository qcmQuestionRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final ApplicationAnswerRepository applicationAnswerRepository;
    private final UserClient userClient;
    private final GoogleMeetService googleMeetService;
    private final InterviewEmailService interviewEmailService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    // ---- Zones (Admin) ----
    @Transactional
    public ZoneResponse createZone(ZoneRequest request) {
        if (zoneRepository.existsByName(request.getName()))
            throw new IllegalArgumentException("Zone name already exists");
        Zone zone = Zone.builder().name(request.getName()).description(request.getDescription()).build();
        return toZoneResponse(zoneRepository.save(zone));
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> getAllZones() {
        return zoneRepository.findByActiveTrue().stream().map(this::toZoneResponse).toList();
    }

    @Transactional
    public List<RhZoneAssignmentResponse> assignRhToZone(RhZoneAssignmentRequest request) {
        ApiResponse<UserDto> user = userClient.getUserById(internalApiKey, request.getRhUserId());
        if (!user.isSuccess() || user.getData() == null)
            throw new IllegalArgumentException("RH user not found");
        if (!"ROLE_RH".equals(user.getData().getRole()))
            throw new IllegalArgumentException("User must have ROLE_RH");

        if (request.getZoneIds() == null || request.getZoneIds().isEmpty()) {
            throw new IllegalArgumentException("At least one zone is required");
        }

        List<RhZoneAssignmentResponse> createdAssignments = new ArrayList<>();
        Set<String> uniqueZoneIds = new LinkedHashSet<>(request.getZoneIds());
        for (String zoneId : uniqueZoneIds) {
            zoneRepository.findByZoneId(zoneId)
                    .orElseThrow(() -> new IllegalArgumentException("Zone not found"));

            RhZoneAssignment existingZoneAssignment = rhZoneAssignmentRepository.findByZoneId(zoneId).orElse(null);
            if (existingZoneAssignment != null && !existingZoneAssignment.getRhUserId().equals(request.getRhUserId())) {
                throw new IllegalArgumentException("One of the selected zones is already assigned to another RH");
            }

            if (!rhZoneAssignmentRepository.existsByRhUserIdAndZoneId(request.getRhUserId(), zoneId)) {
                RhZoneAssignment assignment = rhZoneAssignmentRepository.save(
                        RhZoneAssignment.builder()
                                .rhUserId(request.getRhUserId())
                                .zoneId(zoneId)
                                .build()
                );
                createdAssignments.add(toRhZoneAssignmentResponse(assignment));
            }
        }
        return createdAssignments;
    }

    @Transactional(readOnly = true)
    public List<RhZoneAssignmentResponse> getRhZoneAssignments() {
        return rhZoneAssignmentRepository.findAll().stream()
                .map(this::toRhZoneAssignmentResponse)
                .toList();
    }

    // ---- Companies ----
    @Transactional
    public CompanyResponse createCompany(CompanyRequest request, AuthUser authUser) {
        Zone zone = zoneRepository.findByZoneId(request.getZoneId())
                .orElseThrow(() -> new IllegalArgumentException("Zone not found"));
        if (authUser.isRh()) ensureRhZone(authUser.getUserId(), zone.getZoneId());
        Company company = Company.builder()
                .name(request.getName()).zoneId(zone.getZoneId()).address(request.getAddress()).build();
        return toCompanyResponse(companyRepository.save(company), zone.getName());
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> getCompanies(AuthUser authUser) {
        if (authUser.isAdmin()) {
            return companyRepository.findByActiveTrue().stream()
                    .map(c -> toCompanyResponse(c, getZoneName(c.getZoneId()))).toList();
        }
        if (authUser.isRh()) {
            List<String> zoneIds = getRhZoneIds(authUser.getUserId());
            return companyRepository.findByZoneIdInAndActiveTrue(zoneIds).stream()
                    .map(c -> toCompanyResponse(c, getZoneName(c.getZoneId()))).toList();
        }
        return companyRepository.findByActiveTrue().stream()
                .map(c -> toCompanyResponse(c, getZoneName(c.getZoneId()))).toList();
    }

    // ---- Recruitments ----
    @Transactional
    public RecruitmentResponse createRecruitment(RecruitmentRequest request, AuthUser authUser) {
        Company company = companyRepository.findByCompanyId(request.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        if (authUser.isRh()) ensureRhZone(authUser.getUserId(), company.getZoneId());

        Recruitment recruitment = mapToEntity(request, company);
        recruitment.setCreatedByRhUserId(authUser.getUserId());
        recruitment.setZoneId(company.getZoneId());
        if (recruitment.getStatus() == null) recruitment.setStatus(RecruitmentStatus.DRAFT);
        Recruitment saved = recruitmentRepository.save(recruitment);

        if (request.getQuestions() != null) {
            saveQuestions(saved.getRecruitmentId(), request.getQuestions());
        }
        return toRecruitmentResponse(saved, true);
    }

    @Transactional
    public RecruitmentResponse updateRecruitment(String recruitmentId, RecruitmentRequest request, AuthUser authUser) {
        Recruitment recruitment = getRecruitmentOrThrow(recruitmentId);
        if (authUser.isRh()) ensureRhZone(authUser.getUserId(), recruitment.getZoneId());

        Company company = companyRepository.findByCompanyId(request.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        applyRequest(recruitment, request, company);
        Recruitment saved = recruitmentRepository.save(recruitment);

        if (request.getQuestions() != null) {
            qcmQuestionRepository.deleteByRecruitmentId(recruitmentId);
            saveQuestions(recruitmentId, request.getQuestions());
        }
        return toRecruitmentResponse(saved, true);
    }

    @Transactional(readOnly = true)
    public List<RecruitmentResponse> getRecruitments(AuthUser authUser) {
        if (authUser.isAdmin()) {
            return recruitmentRepository.findAll().stream()
                    .map(r -> toRecruitmentResponse(r, false)).toList();
        }
        if (authUser.isRh()) {
            List<String> zoneIds = getRhZoneIds(authUser.getUserId());
            return recruitmentRepository.findByZoneIdIn(zoneIds).stream()
                    .map(r -> toRecruitmentResponse(r, false)).toList();
        }
        return recruitmentRepository.findByStatus(RecruitmentStatus.PUBLISHED).stream()
                .map(r -> toRecruitmentResponse(r, false)).toList();
    }

    @Transactional(readOnly = true)
    public List<RecruitmentResponse> getPublishedRecruitments() {
        return recruitmentRepository.findByStatus(RecruitmentStatus.PUBLISHED).stream()
                .map(r -> toRecruitmentResponse(r, false)).toList();
    }

    @Transactional(readOnly = true)
    public RecruitmentResponse getRecruitment(String recruitmentId, boolean includeCorrectAnswers) {
        return toRecruitmentResponse(getRecruitmentOrThrow(recruitmentId), includeCorrectAnswers);
    }

    @Transactional(readOnly = true)
    public RecruitmentResponse getRecruitmentForCandidate(String recruitmentId) {
        Recruitment r = getRecruitmentOrThrow(recruitmentId);
        if (r.getStatus() != RecruitmentStatus.PUBLISHED)
            throw new IllegalArgumentException("Recruitment is not published");
        RecruitmentResponse response = toRecruitmentResponse(r, false);
        if (response.getQuestions() != null) {
            response.getQuestions().forEach(q -> q.setCorrectOption(null));
        }
        return response;
    }

    // ---- Applications ----
    @Transactional
    public ApplicationResponse apply(ApplicationRequest request, AuthUser authUser) {
        if (!authUser.isCandidate())
            throw new IllegalArgumentException("Only candidates can apply");
        Recruitment recruitment = getRecruitmentOrThrow(request.getRecruitmentId());
        if (recruitment.getStatus() != RecruitmentStatus.PUBLISHED)
            throw new IllegalArgumentException("Recruitment is not open");
        if (jobApplicationRepository.existsByRecruitmentIdAndCandidateUserId(
                request.getRecruitmentId(), authUser.getUserId()))
            throw new IllegalArgumentException("You already applied for this position");

        List<QcmQuestion> questions = qcmQuestionRepository
                .findByRecruitmentIdOrderByOrderIndexAsc(request.getRecruitmentId());
        if (questions.isEmpty()) throw new IllegalArgumentException("No QCM configured for this recruitment");
        if (request.getAnswers() == null || request.getAnswers().size() != questions.size())
            throw new IllegalArgumentException("All QCM questions must be answered");

        int score = 0;
        JobApplication application = JobApplication.builder()
                .recruitmentId(request.getRecruitmentId())
                .candidateUserId(authUser.getUserId())
                .cvFileUrl(request.getCvFileUrl())
                .qcmTotalQuestions(questions.size())
                .build();
        JobApplication saved = jobApplicationRepository.save(application);

        Map<String, QcmQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(QcmQuestion::getQuestionId, q -> q));
        for (QcmAnswerRequest answer : request.getAnswers()) {
            QcmQuestion question = questionMap.get(answer.getQuestionId());
            if (question == null) throw new IllegalArgumentException("Invalid question id");
            boolean correct = question.getCorrectOption().equalsIgnoreCase(answer.getSelectedOption());
            if (correct) score++;
            applicationAnswerRepository.save(ApplicationAnswer.builder()
                    .applicationId(saved.getApplicationId())
                    .questionId(question.getQuestionId())
                    .selectedOption(answer.getSelectedOption())
                    .correct(correct).build());
        }
        saved.setQcmScore(score);
        jobApplicationRepository.save(saved);
        return toApplicationResponse(saved, recruitment, true);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsForRecruitment(String recruitmentId, AuthUser authUser) {
        Recruitment recruitment = getRecruitmentOrThrow(recruitmentId);
        if (authUser.isRh()) ensureRhZone(authUser.getUserId(), recruitment.getZoneId());
        return jobApplicationRepository.findByRecruitmentId(recruitmentId).stream()
                .map(a -> toApplicationResponse(a, recruitment, true)).toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications(AuthUser authUser) {
        return jobApplicationRepository.findByCandidateUserId(authUser.getUserId()).stream()
                .map(a -> {
                    Recruitment r = getRecruitmentOrThrow(a.getRecruitmentId());
                    return toApplicationResponse(a, r, false);
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsForRh(AuthUser authUser) {
        List<String> zoneIds = getRhZoneIds(authUser.getUserId());
        List<String> recruitmentIds = recruitmentRepository.findByZoneIdIn(zoneIds).stream()
                .map(Recruitment::getRecruitmentId).toList();
        if (recruitmentIds.isEmpty()) return List.of();
        return jobApplicationRepository.findByRecruitmentIdIn(recruitmentIds).stream()
                .map(a -> toApplicationResponse(a, getRecruitmentOrThrow(a.getRecruitmentId()), true))
                .toList();
    }

    @Transactional
    public ApplicationResponse updateApplicationStatus(
            String applicationId, ApplicationStatus status, LocalDateTime interviewAt, AuthUser authUser) {
        if (status != ApplicationStatus.ACCEPTED && status != ApplicationStatus.REJECTED
                && status != ApplicationStatus.UNDER_REVIEW) {
            throw new IllegalArgumentException("Invalid application status");
        }
        JobApplication application = jobApplicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        Recruitment recruitment = getRecruitmentOrThrow(application.getRecruitmentId());
        if (authUser.isRh()) ensureRhZone(authUser.getUserId(), recruitment.getZoneId());
        application.setStatus(status);
        if (status == ApplicationStatus.ACCEPTED) {
            if (interviewAt == null) {
                throw new IllegalArgumentException("Interview date is required when accepting a candidate");
            }
            application.setInterviewAt(interviewAt);

            String candidateEmail = null;
            String candidateName = "Candidat";
            ApiResponse<UserDto> userResp = userClient.getUserById(internalApiKey, application.getCandidateUserId());
            if (userResp.isSuccess() && userResp.getData() != null) {
                UserDto candidate = userResp.getData();
                candidateEmail = candidate.getEmail();
                candidateName = (candidate.getFirstName() != null ? candidate.getFirstName() : "")
                        + " " + (candidate.getLastName() != null ? candidate.getLastName() : "");
                candidateName = candidateName.trim();
                if (candidateName.isBlank()) {
                    candidateName = candidate.getUsername() != null ? candidate.getUsername() : "Candidat";
                }
            }

            String rhEmail = null;
            String rhName = authUser.getUsername();
            ApiResponse<UserDto> rhResp = userClient.getUserById(internalApiKey, authUser.getUserId());
            if (rhResp.isSuccess() && rhResp.getData() != null) {
                UserDto rh = rhResp.getData();
                rhEmail = rh.getEmail();
                rhName = ((rh.getFirstName() != null ? rh.getFirstName() : "")
                        + " " + (rh.getLastName() != null ? rh.getLastName() : "")).trim();
                if (rhName.isBlank()) {
                    rhName = rh.getUsername();
                }
            }

            String meetLink = googleMeetService.createMeetLink(
                    "Entretien — " + recruitment.getTitle(),
                    interviewAt,
                    candidateEmail,
                    candidateName,
                    rhEmail);
            application.setGoogleMeetLink(meetLink);

            interviewEmailService.sendInterviewNotifications(
                    candidateEmail,
                    candidateName,
                    rhEmail,
                    rhName,
                    recruitment.getTitle(),
                    recruitment.getRegion(),
                    interviewAt,
                    meetLink);
        } else if (status == ApplicationStatus.REJECTED) {
            application.setInterviewAt(null);
            application.setGoogleMeetLink(null);
        }
        return toApplicationResponse(jobApplicationRepository.save(application), recruitment, true);
    }

    // ---- Helpers ----
    private void ensureRhZone(String rhUserId, String zoneId) {
        if (!getRhZoneIds(rhUserId).contains(zoneId))
            throw new IllegalArgumentException("Access denied: outside your zone");
    }

    private List<String> getRhZoneIds(String rhUserId) {
        List<String> zoneIds = rhZoneAssignmentRepository.findByRhUserId(rhUserId).stream()
                .map(RhZoneAssignment::getZoneId)
                .toList();
        if (zoneIds.isEmpty())
            throw new IllegalArgumentException("RH has no assigned zone");
        return zoneIds;
    }

    private String getZoneName(String zoneId) {
        return zoneRepository.findByZoneId(zoneId).map(Zone::getName).orElse("");
    }

    private Recruitment getRecruitmentOrThrow(String recruitmentId) {
        return recruitmentRepository.findByRecruitmentId(recruitmentId)
                .orElseThrow(() -> new IllegalArgumentException("Recruitment not found"));
    }

    private void saveQuestions(String recruitmentId, List<QcmQuestionRequest> questions) {
        int index = 0;
        for (QcmQuestionRequest q : questions) {
            qcmQuestionRepository.save(QcmQuestion.builder()
                    .recruitmentId(recruitmentId)
                    .questionText(q.getQuestionText())
                    .optionA(q.getOptionA()).optionB(q.getOptionB())
                    .optionC(q.getOptionC()).optionD(q.getOptionD())
                    .correctOption(q.getCorrectOption().toUpperCase())
                    .orderIndex(q.getOrderIndex() != null ? q.getOrderIndex() : index++)
                    .build());
        }
    }

    private Recruitment mapToEntity(RecruitmentRequest req, Company company) {
        Recruitment r = new Recruitment();
        applyRequest(r, req, company);
        return r;
    }

    private void applyRequest(Recruitment r, RecruitmentRequest req, Company company) {
        r.setTitle(req.getTitle());
        r.setDescription(req.getDescription());
        r.setResponsibilities(req.getResponsibilities());
        r.setTechnicalSkills(req.getTechnicalSkills());
        r.setPersonalSkills(req.getPersonalSkills());
        r.setEducationRequirements(req.getEducationRequirements());
        r.setExperienceRequirements(req.getExperienceRequirements());
        r.setCompanyId(company.getCompanyId());
        r.setZoneId(company.getZoneId());
        r.setJobType(req.getJobType());
        r.setAvailability(req.getAvailability());
        r.setSalaryMin(req.getSalaryMin());
        r.setSalaryMax(req.getSalaryMax());
        r.setSalaryPeriod(req.getSalaryPeriod());
        r.setLanguages(req.getLanguages() != null ? req.getLanguages() : new ArrayList<>());
        r.setEducationLevel(req.getEducationLevel());
        r.setExperienceLevel(req.getExperienceLevel());
        r.setDrivingLicenseRequired(req.getDrivingLicenseRequired());
        r.setCountry(req.getCountry());
        r.setRegion(req.getRegion());
        r.setCity(req.getCity());
        r.setLocalTravel(req.getLocalTravel());
        r.setInternationalTravel(req.getInternationalTravel());
        r.setAnonymousMode(req.getAnonymousMode());
        r.setPublicationDate(req.getPublicationDate());
        r.setResponsibleName(req.getResponsibleName());
        r.setEmailNotificationPerApplication(req.getEmailNotificationPerApplication());
        r.setInternalReference(req.getInternalReference());
        r.setKeejobReference(req.getKeejobReference());
        if (req.getStatus() != null) r.setStatus(req.getStatus());
    }

    private ZoneResponse toZoneResponse(Zone zone) {
        return ZoneResponse.builder().zoneId(zone.getZoneId()).name(zone.getName())
                .description(zone.getDescription()).active(zone.isActive()).build();
    }

    private CompanyResponse toCompanyResponse(Company c, String zoneName) {
        return CompanyResponse.builder().companyId(c.getCompanyId()).name(c.getName())
                .zoneId(c.getZoneId()).zoneName(zoneName).address(c.getAddress()).active(c.isActive()).build();
    }

    private RhZoneAssignmentResponse toRhZoneAssignmentResponse(RhZoneAssignment assignment) {
        return RhZoneAssignmentResponse.builder()
                .id(assignment.getId())
                .rhUserId(assignment.getRhUserId())
                .zoneId(assignment.getZoneId())
                .zoneName(getZoneName(assignment.getZoneId()))
                .assignedAt(assignment.getAssignedAt())
                .build();
    }

    private RecruitmentResponse toRecruitmentResponse(Recruitment r, boolean includeCorrect) {
        String companyName = companyRepository.findByCompanyId(r.getCompanyId()).map(Company::getName).orElse("");
        String zoneName = getZoneName(r.getZoneId());
        List<QcmQuestionResponse> questions = qcmQuestionRepository
                .findByRecruitmentIdOrderByOrderIndexAsc(r.getRecruitmentId()).stream()
                .map(q -> QcmQuestionResponse.builder()
                        .questionId(q.getQuestionId()).questionText(q.getQuestionText())
                        .optionA(q.getOptionA()).optionB(q.getOptionB())
                        .optionC(q.getOptionC()).optionD(q.getOptionD())
                        .correctOption(includeCorrect ? q.getCorrectOption() : null)
                        .orderIndex(q.getOrderIndex()).build())
                .toList();

        return RecruitmentResponse.builder()
                .recruitmentId(r.getRecruitmentId()).title(r.getTitle())
                .description(r.getDescription()).responsibilities(r.getResponsibilities())
                .technicalSkills(r.getTechnicalSkills()).personalSkills(r.getPersonalSkills())
                .educationRequirements(r.getEducationRequirements())
                .experienceRequirements(r.getExperienceRequirements())
                .companyId(r.getCompanyId()).companyName(companyName)
                .zoneId(r.getZoneId()).zoneName(zoneName)
                .jobType(r.getJobType()).availability(r.getAvailability())
                .salaryMin(r.getSalaryMin()).salaryMax(r.getSalaryMax()).salaryPeriod(r.getSalaryPeriod())
                .languages(r.getLanguages() != null ? new ArrayList<>(r.getLanguages()) : List.of())
                .educationLevel(r.getEducationLevel())
                .experienceLevel(r.getExperienceLevel()).drivingLicenseRequired(r.getDrivingLicenseRequired())
                .country(r.getCountry()).region(r.getRegion()).city(r.getCity())
                .localTravel(r.getLocalTravel()).internationalTravel(r.getInternationalTravel())
                .anonymousMode(r.getAnonymousMode()).publicationDate(r.getPublicationDate())
                .responsibleName(r.getResponsibleName())
                .internalReference(r.getInternalReference()).keejobReference(r.getKeejobReference())
                .status(r.getStatus()).createdAt(r.getCreatedAt()).questions(questions)
                .build();
    }

    private ApplicationResponse toApplicationResponse(JobApplication a, Recruitment r, boolean includeDetails) {
        UserSummary candidate = null;
        if (includeDetails) {
            ApiResponse<UserDto> userResp = userClient.getUserById(internalApiKey, a.getCandidateUserId());
            if (userResp.isSuccess() && userResp.getData() != null) {
                UserDto u = userResp.getData();
                candidate = UserSummary.builder().userId(u.getUserId())
                        .firstName(u.getFirstName()).lastName(u.getLastName())
                        .username(u.getUsername()).email(u.getEmail())
                        .phoneNumber(u.getPhoneNumber()).profileImageUrl(u.getProfileImageUrl()).build();
            }
        }

        List<ApplicationAnswerResponse> answers = applicationAnswerRepository.findByApplicationId(a.getApplicationId())
                .stream().map(ans -> {
                    QcmQuestion q = qcmQuestionRepository.findByQuestionId(ans.getQuestionId()).orElse(null);
                    return ApplicationAnswerResponse.builder()
                            .questionId(ans.getQuestionId())
                            .questionText(q != null ? q.getQuestionText() : "")
                            .selectedOption(ans.getSelectedOption())
                            .correctOption(includeDetails && q != null ? q.getCorrectOption() : null)
                            .correct(ans.isCorrect()).build();
                }).toList();

        return ApplicationResponse.builder()
                .applicationId(a.getApplicationId()).recruitmentId(a.getRecruitmentId())
                .recruitmentTitle(r.getTitle()).zoneName(getZoneName(r.getZoneId())).region(r.getRegion())
                .candidateUserId(a.getCandidateUserId())
                .candidate(candidate).cvFileUrl(a.getCvFileUrl()).status(a.getStatus())
                .qcmScore(a.getQcmScore()).qcmTotalQuestions(a.getQcmTotalQuestions())
                .interviewAt(a.getInterviewAt()).googleMeetLink(a.getGoogleMeetLink())
                .appliedAt(a.getAppliedAt()).answers(includeDetails ? answers : null)
                .build();
    }
}
