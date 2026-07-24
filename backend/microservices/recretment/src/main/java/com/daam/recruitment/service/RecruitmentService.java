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
import org.springframework.util.StringUtils;
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
    private final QcmRepository qcmRepository;
    private final QcmQuestionRepository qcmQuestionRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final ApplicationAnswerRepository applicationAnswerRepository;
    private final InterviewRepository interviewRepository;
    private final UserClient userClient;
    private final InterviewEmailService interviewEmailService;
    private final CvAnalysisService cvAnalysisService;
    private final InterviewService interviewService;

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
                .name(request.getName())
                .zoneId(zone.getZoneId())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .googleMapsUrl(request.getGoogleMapsUrl())
                .build();
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
        assignQcmIfPresent(recruitment, request.getQcmId());
        Recruitment saved = recruitmentRepository.save(recruitment);
        return toRecruitmentResponse(saved, true);
    }

    @Transactional
    public RecruitmentResponse updateRecruitment(String recruitmentId, RecruitmentRequest request, AuthUser authUser) {
        Recruitment recruitment = getRecruitmentOrThrow(recruitmentId);
        if (authUser.isRh()) ensureRhZone(authUser.getUserId(), recruitment.getZoneId());

        Company company = companyRepository.findByCompanyId(request.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        applyRequest(recruitment, request, company);
        assignQcmIfPresent(recruitment, request.getQcmId());
        Recruitment saved = recruitmentRepository.save(recruitment);
        return toRecruitmentResponse(saved, true);
    }

    @Transactional
    public void deleteRecruitment(String recruitmentId, AuthUser authUser) {
        Recruitment recruitment = getRecruitmentOrThrow(recruitmentId);
        if (authUser.isRh()) {
            ensureRhZone(authUser.getUserId(), recruitment.getZoneId());
        }

        List<JobApplication> applications = jobApplicationRepository.findByRecruitmentId(recruitmentId);
        for (JobApplication application : applications) {
            String applicationId = application.getApplicationId();
            applicationAnswerRepository.deleteByApplicationId(applicationId);
            interviewRepository.deleteByApplicationId(applicationId);
            jobApplicationRepository.delete(application);
        }
        recruitmentRepository.delete(recruitment);
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

        List<QcmQuestion> questions = getQuestionsForRecruitment(recruitment);
        if (questions.isEmpty()) throw new IllegalArgumentException("No QCM configured for this recruitment");

        boolean violated = Boolean.TRUE.equals(request.getQcmViolated());
        if (!violated && (request.getAnswers() == null || request.getAnswers().size() != questions.size())) {
            throw new IllegalArgumentException("All QCM questions must be answered");
        }

        Map<String, QcmAnswerRequest> answerMap = request.getAnswers() == null
                ? Map.of()
                : request.getAnswers().stream()
                        .filter(a -> a.getQuestionId() != null)
                        .collect(Collectors.toMap(QcmAnswerRequest::getQuestionId, a -> a, (a, b) -> a));

        int score = 0;
        JobApplication application = JobApplication.builder()
                .recruitmentId(request.getRecruitmentId())
                .candidateUserId(authUser.getUserId())
                .cvFileUrl(request.getCvFileUrl())
                .qcmTotalQuestions(questions.size())
                .build();
        JobApplication saved = jobApplicationRepository.save(application);

        for (QcmQuestion question : questions) {
            QcmAnswerRequest answer = answerMap.get(question.getQuestionId());
            String selected = answer != null ? answer.getSelectedOption() : null;
            boolean correct = !violated
                    && selected != null
                    && question.getCorrectOption().equalsIgnoreCase(selected);
            if (correct) score++;
            applicationAnswerRepository.save(ApplicationAnswer.builder()
                    .applicationId(saved.getApplicationId())
                    .questionId(question.getQuestionId())
                    .selectedOption(selected != null ? selected : "-")
                    .correct(correct).build());
        }
        saved.setQcmScore(violated ? 0 : score);
        jobApplicationRepository.save(saved);
        try {
            saved = cvAnalysisService.analyzeApplication(saved.getApplicationId());
        } catch (Exception e) {
            // Score stays null only if analyze itself could not persist a fallback
        }
        return toApplicationResponse(saved, recruitment, true);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsForRecruitment(String recruitmentId, AuthUser authUser) {
        Recruitment recruitment = getRecruitmentOrThrow(recruitmentId);
        if (authUser.isRh()) ensureRhZone(authUser.getUserId(), recruitment.getZoneId());
        return jobApplicationRepository.findByRecruitmentId(recruitmentId).stream()
                .map(a -> toApplicationResponse(a, recruitment, true))
                .sorted(applicationRankingComparator())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications(AuthUser authUser) {
        return jobApplicationRepository.findByCandidateUserId(authUser.getUserId()).stream()
                .map(a -> {
                    Recruitment r = getRecruitmentOrThrow(a.getRecruitmentId());
                    return toApplicationResponse(a, r, false);
                }).toList();
    }

    @Transactional
    public List<ApplicationResponse> getApplicationsForRh(AuthUser authUser) {
        List<String> zoneIds = getRhZoneIds(authUser.getUserId());
        List<String> recruitmentIds = recruitmentRepository.findByZoneIdIn(zoneIds).stream()
                .map(Recruitment::getRecruitmentId).toList();
        if (recruitmentIds.isEmpty()) return List.of();
        return jobApplicationRepository.findByRecruitmentIdIn(recruitmentIds).stream()
                .map(a -> ensureCvAnalyzed(a))
                .map(a -> toApplicationResponse(a, getRecruitmentOrThrow(a.getRecruitmentId()), true))
                .sorted(applicationRankingComparator())
                .toList();
    }

    @Transactional
    public ApplicationResponse updateApplicationTracking(
            String applicationId,
            ApplicationTrackingUpdateRequest request,
            AuthUser authUser) {
        JobApplication application = jobApplicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        Recruitment recruitment = getRecruitmentOrThrow(application.getRecruitmentId());
        if (authUser.isRh()) {
            ensureRhZone(authUser.getUserId(), recruitment.getZoneId());
        }
        if (request.getProvenance() != null) application.setProvenance(blankToNull(request.getProvenance()));
        if (request.getImf() != null) application.setImf(blankToNull(request.getImf()));
        if (request.getProfilMetier() != null) application.setProfilMetier(blankToNull(request.getProfilMetier()));
        if (request.getExperienceYears() != null) application.setExperienceYears(blankToNull(request.getExperienceYears()));
        if (request.getSituationPerso() != null) application.setSituationPerso(blankToNull(request.getSituationPerso()));
        if (request.getSalaireActuel() != null) application.setSalaireActuel(blankToNull(request.getSalaireActuel()));
        if (request.getDisponibilite() != null) application.setDisponibilite(blankToNull(request.getDisponibilite()));
        if (request.getCommentairesRh() != null) application.setCommentairesRh(blankToNull(request.getCommentairesRh()));
        if (request.getContactName() != null) application.setContactName(blankToNull(request.getContactName()));
        if (request.getCommercialName() != null) application.setCommercialName(blankToNull(request.getCommercialName()));
        if (request.getCodeDossier() != null) application.setCodeDossier(blankToNull(request.getCodeDossier()));
        if (request.getDiplomeEcole() != null) application.setDiplomeEcole(blankToNull(request.getDiplomeEcole()));
        if (request.getFormatMission() != null) application.setFormatMission(blankToNull(request.getFormatMission()));
        if (request.getDateDebutMission() != null) application.setDateDebutMission(request.getDateDebutMission());
        if (request.getDateFinMission() != null) application.setDateFinMission(request.getDateFinMission());
        if (request.getDureeContrat() != null) application.setDureeContrat(blankToNull(request.getDureeContrat()));
        if (request.getPrixMois() != null) application.setPrixMois(blankToNull(request.getPrixMois()));
        if (request.getCompetence() != null) application.setCompetence(blankToNull(request.getCompetence()));
        if (request.getANegocier() != null) application.setANegocier(blankToNull(request.getANegocier()));
        if (request.getDeplacement() != null) application.setDeplacement(blankToNull(request.getDeplacement()));
        if (request.getPretention() != null) application.setPretention(blankToNull(request.getPretention()));
        if (request.getRemarquesRh() != null) application.setRemarquesRh(blankToNull(request.getRemarquesRh()));
        if (request.getAffectation() != null) application.setAffectation(blankToNull(request.getAffectation()));
        if (request.getDesistement() != null) application.setDesistement(blankToNull(request.getDesistement()));
        if (request.getComposante() != null) application.setComposante(blankToNull(request.getComposante()));
        if (request.getObservation() != null) application.setObservation(blankToNull(request.getObservation()));
        return toApplicationResponse(jobApplicationRepository.save(application), recruitment, true);
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private JobApplication ensureCvAnalyzed(JobApplication application) {
        if (application.getCvMatchScore() != null || application.getCvFileUrl() == null
                || application.getCvFileUrl().isBlank()) {
            return application;
        }
        try {
            return cvAnalysisService.analyzeApplication(application.getApplicationId());
        } catch (Exception e) {
            return application;
        }
    }

    @Transactional
    public ApplicationResponse analyzeApplicationCv(String applicationId, AuthUser authUser) {
        JobApplication application = jobApplicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        Recruitment recruitment = getRecruitmentOrThrow(application.getRecruitmentId());
        if (authUser.isRh()) ensureRhZone(authUser.getUserId(), recruitment.getZoneId());
        JobApplication analyzed = cvAnalysisService.analyzeApplication(applicationId);
        return toApplicationResponse(analyzed, recruitment, true);
    }

    @Transactional
    public ApplicationResponse updateApplicationStatus(
            String applicationId,
            ApplicationStatusUpdateRequest request,
            AuthUser authUser) {
        ApplicationStatus status = request.getStatus();
        if (status != ApplicationStatus.ACCEPTED && status != ApplicationStatus.REJECTED
                && status != ApplicationStatus.UNDER_REVIEW && status != ApplicationStatus.HIRED) {
            throw new IllegalArgumentException("Invalid application status");
        }
        JobApplication application = jobApplicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        Recruitment recruitment = getRecruitmentOrThrow(application.getRecruitmentId());
        if (authUser.isRh()) ensureRhZone(authUser.getUserId(), recruitment.getZoneId());

        ApplicationStatus previousStatus = application.getStatus();
        boolean physical = request.getInterviewType() != null
                && "PHYSICAL".equalsIgnoreCase(request.getInterviewType().trim());

        if (physical && previousStatus != ApplicationStatus.ACCEPTED) {
            throw new IllegalArgumentException(
                    "Planifiez d'abord l'entretien en ligne avant la reunion physique");
        }

        if (status == ApplicationStatus.HIRED) {
            return confirmHire(application, recruitment, previousStatus, request, authUser);
        }

        application.setStatus(status);

        if (status == ApplicationStatus.ACCEPTED) {
            LocalDateTime interviewAt = request.getInterviewAt();
            LocalDateTime interviewEndAt = request.getInterviewEndAt();
            Integer durationMinutes = request.getDurationMinutes();
            String interviewLocation = request.getInterviewLocation();

            if (interviewAt == null) {
                throw new IllegalArgumentException("Interview date is required when accepting a candidate");
            }
            LocalDateTime endAt = interviewEndAt;
            if (endAt == null && durationMinutes != null && durationMinutes > 0) {
                endAt = interviewAt.plusMinutes(durationMinutes);
            }

            CandidateContact candidate = resolveCandidate(application.getCandidateUserId());
            RhContact rh = resolveRh(authUser);

            var scheduleResult = interviewService.scheduleInterview(
                    application,
                    recruitment,
                    authUser.getUserId(),
                    candidate.name(),
                    interviewAt,
                    endAt,
                    physical ? null : rh.meetingLink(),
                    physical);

            Company company = companyRepository.findByCompanyId(recruitment.getCompanyId()).orElse(null);
            String companyName = company != null ? company.getName() : "";
            String companyAddress = company != null && company.getAddress() != null
                    ? company.getAddress()
                    : "";

            if (physical) {
                String lieu = StringUtils.hasText(interviewLocation)
                        ? interviewLocation.trim()
                        : companyAddress;
                if (!StringUtils.hasText(lieu)) {
                    lieu = recruitment.getCity() != null ? recruitment.getCity() : "";
                    if (StringUtils.hasText(recruitment.getRegion())) {
                        lieu = StringUtils.hasText(lieu)
                                ? lieu + ", " + recruitment.getRegion()
                                : recruitment.getRegion();
                    }
                }
                interviewEmailService.sendPhysicalInterviewInvitation(
                        candidate.email(),
                        candidate.name(),
                        rh.email(),
                        rh.name(),
                        recruitment.getTitle(),
                        companyName,
                        companyAddress,
                        lieu,
                        scheduleResult.getInterview().getStartDateTime());
            } else {
                interviewEmailService.sendInterviewNotifications(
                        candidate.email(),
                        candidate.name(),
                        rh.email(),
                        rh.name(),
                        recruitment.getTitle(),
                        recruitment.getRegion(),
                        scheduleResult.getInterview().getStartDateTime(),
                        scheduleResult.getInterview().getEndDateTime(),
                        scheduleResult.getMeetingLink(),
                        scheduleResult.getMeetingProvider(),
                        scheduleResult.getWarningMessage());
            }

            JobApplication saved = jobApplicationRepository.save(application);
            ApplicationResponse response = toApplicationResponse(saved, recruitment, true);
            if (scheduleResult.getWarningMessage() != null) {
                response.setMeetingWarning(scheduleResult.getWarningMessage());
            }
            return response;
        }

        if (status == ApplicationStatus.REJECTED) {
            interviewService.cancelByApplication(application.getApplicationId());
            application.setInterviewAt(null);
            application.setInterviewEndAt(null);
            application.setGoogleMeetLink(null);
            application.setMeetingProvider(null);
            application.setMeetingId(null);
            application.setMeetingWarning(null);
        }
        return toApplicationResponse(jobApplicationRepository.save(application), recruitment, true);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getHiredApplicationsForRh(AuthUser authUser) {
        return getApplicationsForRh(authUser).stream()
                .filter(a -> a.getStatus() == ApplicationStatus.HIRED)
                .toList();
    }

    private ApplicationResponse confirmHire(
            JobApplication application,
            Recruitment recruitment,
            ApplicationStatus previousStatus,
            ApplicationStatusUpdateRequest request,
            AuthUser authUser) {
        if (previousStatus != ApplicationStatus.ACCEPTED) {
            throw new IllegalArgumentException(
                    "La confirmation d'embauche n'est possible qu'apres l'acceptation du candidat");
        }
        if (!"PHYSICAL".equalsIgnoreCase(application.getMeetingProvider())) {
            throw new IllegalArgumentException(
                    "Planifiez d'abord l'entretien physique avant la confirmation d'embauche");
        }
        if (request.getHireStartDate() == null) {
            throw new IllegalArgumentException("La date de prise de fonction est obligatoire");
        }
        if (!StringUtils.hasText(request.getHireNetSalary())) {
            throw new IllegalArgumentException("La remuneration nette mensuelle est obligatoire");
        }

        Company company = companyRepository.findByCompanyId(recruitment.getCompanyId()).orElse(null);
        String companyName = company != null ? company.getName() : "";
        String companyAddress = company != null && StringUtils.hasText(company.getAddress())
                ? company.getAddress().trim()
                : "";
        String companyMaps = company != null && StringUtils.hasText(company.getGoogleMapsUrl())
                ? company.getGoogleMapsUrl().trim()
                : "";

        String integrationAddress = companyAddress;
        String integrationGps = companyMaps;

        String contractType = StringUtils.hasText(request.getHireContractType())
                ? request.getHireContractType().trim()
                : "Contrat à durée indéterminée (CDI), assorti d'une période d'essai de six (6) mois, renouvelable une seule fois, sous réserve d'éligibilité au contrat CIVP";
        String workingHours = StringUtils.hasText(request.getHireWorkingHours())
                ? request.getHireWorkingHours().trim()
                : "08 heures par jour, du lundi au vendredi de 8h à 17h30, avec permanence le samedi de fin de mois de 08h00 à 12h00";
        String benefits = StringUtils.hasText(request.getHireBenefits())
                ? request.getHireBenefits().trim()
                : DEFAULT_HIRE_BENEFITS;

        application.setStatus(ApplicationStatus.HIRED);
        application.setHiredAt(LocalDateTime.now());
        application.setHireStartDate(request.getHireStartDate());
        application.setHireContractType(contractType);
        application.setHireNetSalary(request.getHireNetSalary().trim());
        application.setHireWorkingHours(workingHours);
        application.setHireBenefits(benefits);
        application.setHireIntegrationAddress(integrationAddress);
        application.setHireIntegrationGpsUrl(integrationGps);

        CandidateContact candidate = resolveCandidate(application.getCandidateUserId());
        RhContact rh = resolveRh(authUser);

        interviewEmailService.sendHireConfirmation(
                candidate.email(),
                candidate.name(),
                rh.email(),
                rh.name(),
                recruitment.getTitle(),
                companyName,
                request.getHireStartDate(),
                contractType,
                workingHours,
                request.getHireNetSalary().trim(),
                benefits,
                integrationAddress,
                integrationGps);

        return toApplicationResponse(jobApplicationRepository.save(application), recruitment, true);
    }

    private static final String DEFAULT_HIRE_BENEFITS = """
            Prime de performance selon les résultats réalisés ;
            Une allocation de 105 DT par mois, à partir de trois (3) dossiers déboursés minimum et jusqu’à 100 000 DT d’encours ;
            Prime de portefeuille mensuelle calculée selon l’évolution du portefeuille, conformément aux dix (10) paliers définis ;
            Tickets restaurant d’une valeur mensuelle de 170 DT ;
            Assurance groupe avec un plafond annuel de remboursement fixé à 6 500 DT.
            """.trim();

    private record CandidateContact(String email, String name) {}
    private record RhContact(String email, String name, String meetingLink) {}

    private CandidateContact resolveCandidate(String candidateUserId) {
        String email = null;
        String name = "Candidat";
        ApiResponse<UserDto> userResp = userClient.getUserById(internalApiKey, candidateUserId);
        if (userResp.isSuccess() && userResp.getData() != null) {
            UserDto candidate = userResp.getData();
            email = candidate.getEmail();
            name = ((candidate.getFirstName() != null ? candidate.getFirstName() : "")
                    + " " + (candidate.getLastName() != null ? candidate.getLastName() : "")).trim();
            if (name.isBlank()) {
                name = candidate.getUsername() != null ? candidate.getUsername() : "Candidat";
            }
        }
        return new CandidateContact(email, name);
    }

    private RhContact resolveRh(AuthUser authUser) {
        String email = null;
        String name = authUser.getUsername();
        String meetingLink = null;
        ApiResponse<UserDto> rhResp = userClient.getUserById(internalApiKey, authUser.getUserId());
        if (rhResp.isSuccess() && rhResp.getData() != null) {
            UserDto rh = rhResp.getData();
            email = rh.getEmail();
            meetingLink = rh.getMeetingLink();
            name = ((rh.getFirstName() != null ? rh.getFirstName() : "")
                    + " " + (rh.getLastName() != null ? rh.getLastName() : "")).trim();
            if (name.isBlank()) {
                name = rh.getUsername();
            }
        }
        return new RhContact(email, name, meetingLink);
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

    private void assignQcmIfPresent(Recruitment recruitment, String qcmId) {
        if (qcmId == null || qcmId.isBlank()) {
            recruitment.setQcmId(null);
            return;
        }
        if (!qcmRepository.existsByQcmId(qcmId)) {
            throw new IllegalArgumentException("QCM not found");
        }
        recruitment.setQcmId(qcmId);
    }

    private List<QcmQuestion> getQuestionsForRecruitment(Recruitment recruitment) {
        if (recruitment.getQcmId() == null || recruitment.getQcmId().isBlank()) {
            return List.of();
        }
        return qcmQuestionRepository.findByQcmIdOrderByOrderIndexAsc(recruitment.getQcmId());
    }

    private Recruitment getRecruitmentOrThrow(String recruitmentId) {
        return recruitmentRepository.findByRecruitmentId(recruitmentId)
                .orElseThrow(() -> new IllegalArgumentException("Recruitment not found"));
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
                .zoneId(c.getZoneId()).zoneName(zoneName).address(c.getAddress())
                .latitude(c.getLatitude()).longitude(c.getLongitude())
                .googleMapsUrl(c.getGoogleMapsUrl())
                .active(c.isActive()).build();
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
        String qcmTitle = null;
        if (r.getQcmId() != null && !r.getQcmId().isBlank()) {
            qcmTitle = qcmRepository.findByQcmId(r.getQcmId()).map(Qcm::getTitle).orElse(null);
        }
        List<QcmQuestionResponse> questions = getQuestionsForRecruitment(r).stream()
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
                .status(r.getStatus()).createdAt(r.getCreatedAt())
                .qcmId(r.getQcmId()).qcmTitle(qcmTitle)
                .questions(questions)
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

        Company company = companyRepository.findByCompanyId(r.getCompanyId()).orElse(null);

        return ApplicationResponse.builder()
                .applicationId(a.getApplicationId()).recruitmentId(a.getRecruitmentId())
                .recruitmentTitle(r.getTitle()).zoneName(getZoneName(r.getZoneId()))
                .region(r.getRegion()).city(r.getCity())
                .candidateUserId(a.getCandidateUserId())
                .candidate(candidate).cvFileUrl(a.getCvFileUrl()).status(a.getStatus())
                .qcmScore(a.getQcmScore()).qcmTotalQuestions(a.getQcmTotalQuestions())
                .cvMatchScore(a.getCvMatchScore())
                .extractedSkills(a.getExtractedSkills())
                .matchedSkills(a.getMatchedSkills())
                .missingSkills(a.getMissingSkills())
                .cvAnalysisSummary(a.getCvAnalysisSummary())
                .cvAnalyzedAt(a.getCvAnalyzedAt())
                .interviewAt(a.getInterviewAt())
                .interviewEndAt(a.getInterviewEndAt())
                .googleMeetLink(a.getGoogleMeetLink())
                .meetingProvider(a.getMeetingProvider())
                .meetingId(a.getMeetingId())
                .meetingWarning(a.getMeetingWarning())
                .companyName(company != null ? company.getName() : null)
                .companyAddress(company != null ? company.getAddress() : null)
                .companyGoogleMapsUrl(company != null ? company.getGoogleMapsUrl() : null)
                .hiredAt(a.getHiredAt())
                .hireStartDate(a.getHireStartDate())
                .hireContractType(a.getHireContractType())
                .hireNetSalary(a.getHireNetSalary())
                .hireWorkingHours(a.getHireWorkingHours())
                .hireBenefits(a.getHireBenefits())
                .hireIntegrationAddress(a.getHireIntegrationAddress())
                .hireIntegrationGpsUrl(a.getHireIntegrationGpsUrl())
                .provenance(a.getProvenance())
                .imf(a.getImf())
                .profilMetier(a.getProfilMetier())
                .experienceYears(a.getExperienceYears())
                .situationPerso(a.getSituationPerso())
                .salaireActuel(a.getSalaireActuel())
                .disponibilite(a.getDisponibilite())
                .commentairesRh(a.getCommentairesRh())
                .contactName(a.getContactName())
                .commercialName(a.getCommercialName())
                .codeDossier(a.getCodeDossier())
                .diplomeEcole(a.getDiplomeEcole())
                .formatMission(a.getFormatMission())
                .dateDebutMission(a.getDateDebutMission())
                .dateFinMission(a.getDateFinMission())
                .dureeContrat(a.getDureeContrat())
                .prixMois(a.getPrixMois())
                .competence(a.getCompetence())
                .aNegocier(a.getANegocier())
                .deplacement(a.getDeplacement())
                .pretention(a.getPretention())
                .remarquesRh(a.getRemarquesRh())
                .affectation(a.getAffectation())
                .desistement(a.getDesistement())
                .composante(a.getComposante())
                .observation(a.getObservation())
                .keejobReference(r.getKeejobReference())
                .internalReference(r.getInternalReference())
                .appliedAt(a.getAppliedAt()).answers(includeDetails ? answers : null)
                .build();
    }

    private Comparator<ApplicationResponse> applicationRankingComparator() {
        return Comparator
                .comparing((ApplicationResponse a) -> a.getCvMatchScore() == null ? -1 : a.getCvMatchScore(),
                        Comparator.reverseOrder())
                .thenComparing(a -> a.getQcmScore() == null ? -1 : a.getQcmScore(), Comparator.reverseOrder())
                .thenComparing(ApplicationResponse::getAppliedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
