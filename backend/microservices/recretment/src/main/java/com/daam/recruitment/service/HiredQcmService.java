package com.daam.recruitment.service;

import com.daam.recruitment.client.UserClient;
import com.daam.recruitment.client.UserDto;
import com.daam.recruitment.dto.RecruitmentDtos.*;
import com.daam.recruitment.entity.*;
import com.daam.recruitment.enumeration.ApplicationStatus;
import com.daam.recruitment.enumeration.HiredQcmStatus;
import com.daam.recruitment.psychometric.PsychometricDimensions;
import com.daam.recruitment.psychometric.PsychometricDimensions.DimensionDef;
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
public class HiredQcmService {

    private final HiredQcmAssignmentRepository assignmentRepository;
    private final HiredQcmAnswerRepository answerRepository;
    private final HiredQcmDimensionScoreRepository dimensionScoreRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final RecruitmentRepository recruitmentRepository;
    private final CompanyRepository companyRepository;
    private final QcmRepository qcmRepository;
    private final QcmQuestionRepository qcmQuestionRepository;
    private final RhZoneAssignmentRepository rhZoneAssignmentRepository;
    private final UserClient userClient;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @Transactional
    public HiredQcmAssignmentResponse assignToHiredCandidate(
            String applicationId,
            AssignHiredQcmRequest request,
            AuthUser authUser) {
        JobApplication application = jobApplicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        if (application.getStatus() != ApplicationStatus.HIRED) {
            throw new IllegalArgumentException("Le QCM ne peut être assigné qu'à un candidat admis (embauché)");
        }

        Recruitment recruitment = recruitmentRepository.findByRecruitmentId(application.getRecruitmentId())
                .orElseThrow(() -> new IllegalArgumentException("Recruitment not found"));
        ensureRhZone(authUser.getUserId(), recruitment.getZoneId());

        Qcm qcm = qcmRepository.findByQcmId(request.getQcmId())
                .orElseThrow(() -> new IllegalArgumentException("QCM not found"));
        List<QcmQuestion> questions = qcmQuestionRepository.findByQcmIdOrderByOrderIndexAsc(qcm.getQcmId());
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("Ce QCM ne contient aucune question");
        }

        if (assignmentRepository.existsByApplicationIdAndQcmIdAndStatus(
                applicationId, qcm.getQcmId(), HiredQcmStatus.ASSIGNED)) {
            throw new IllegalArgumentException("Ce QCM est déjà assigné et en attente pour ce candidat");
        }

        HiredQcmAssignment saved = assignmentRepository.save(HiredQcmAssignment.builder()
                .applicationId(application.getApplicationId())
                .candidateUserId(application.getCandidateUserId())
                .recruitmentId(application.getRecruitmentId())
                .qcmId(qcm.getQcmId())
                .assignedByRhUserId(authUser.getUserId())
                .status(HiredQcmStatus.ASSIGNED)
                .totalQuestions(questions.size())
                .build());

        return toResponse(saved, false, true);
    }

    @Transactional(readOnly = true)
    public List<HiredQcmAssignmentResponse> listForApplication(String applicationId, AuthUser authUser) {
        JobApplication application = jobApplicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        Recruitment recruitment = recruitmentRepository.findByRecruitmentId(application.getRecruitmentId())
                .orElseThrow(() -> new IllegalArgumentException("Recruitment not found"));
        if (authUser.isRh()) {
            ensureRhZone(authUser.getUserId(), recruitment.getZoneId());
        }
        return assignmentRepository.findByApplicationIdOrderByAssignedAtDesc(applicationId).stream()
                .map(a -> toResponse(a, false, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HiredQcmAssignmentResponse> listMyAssignments(AuthUser authUser) {
        if (!authUser.isCandidate()) {
            throw new IllegalArgumentException("Only candidates can list their evaluations");
        }
        return assignmentRepository.findByCandidateUserIdOrderByAssignedAtDesc(authUser.getUserId()).stream()
                .map(a -> toResponse(a, false, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public HiredQcmAssignmentResponse getAssignmentForCandidate(String assignmentId, AuthUser authUser) {
        HiredQcmAssignment assignment = getAssignmentOrThrow(assignmentId);
        ensureCandidateOwner(assignment, authUser);
        boolean includeQuestions = assignment.getStatus() == HiredQcmStatus.ASSIGNED;
        return toResponse(assignment, includeQuestions, true);
    }

    @Transactional(readOnly = true)
    public HiredQcmAssignment getCompletedAssignmentForRh(String assignmentId, AuthUser authUser) {
        HiredQcmAssignment assignment = getAssignmentOrThrow(assignmentId);
        if (assignment.getStatus() == HiredQcmStatus.ASSIGNED) {
            throw new IllegalArgumentException("L'évaluation n'est pas encore terminée");
        }
        JobApplication application = jobApplicationRepository.findByApplicationId(assignment.getApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        Recruitment recruitment = recruitmentRepository.findByRecruitmentId(application.getRecruitmentId())
                .orElseThrow(() -> new IllegalArgumentException("Recruitment not found"));
        ensureRhZone(authUser.getUserId(), recruitment.getZoneId());
        return assignment;
    }

    @Transactional(readOnly = true)
    public List<HiredQcmAssignment> listCompletedForRhZone(AuthUser authUser) {
        Set<String> zoneIds = rhZoneAssignmentRepository.findByRhUserId(authUser.getUserId()).stream()
                .map(RhZoneAssignment::getZoneId)
                .collect(Collectors.toSet());
        if (zoneIds.isEmpty()) {
            return List.of();
        }
        return assignmentRepository.findByStatusInOrderByCompletedAtDesc(
                        List.of(HiredQcmStatus.COMPLETED, HiredQcmStatus.VIOLATED))
                .stream()
                .filter(a -> {
                    Recruitment r = recruitmentRepository.findByRecruitmentId(a.getRecruitmentId()).orElse(null);
                    return r != null && r.getZoneId() != null && zoneIds.contains(r.getZoneId());
                })
                .toList();
    }

    @Transactional
    public HiredQcmAssignmentResponse submitAssignment(
            String assignmentId,
            HiredQcmSubmitRequest request,
            AuthUser authUser) {
        HiredQcmAssignment assignment = getAssignmentOrThrow(assignmentId);
        ensureCandidateOwner(assignment, authUser);
        if (assignment.getStatus() != HiredQcmStatus.ASSIGNED) {
            throw new IllegalArgumentException("Cette évaluation est déjà terminée");
        }

        List<QcmQuestion> questions = qcmQuestionRepository.findByQcmIdOrderByOrderIndexAsc(assignment.getQcmId());
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("QCM without questions");
        }

        boolean violated = Boolean.TRUE.equals(request.getQcmViolated());
        if (!violated && (request.getAnswers() == null || request.getAnswers().size() != questions.size())) {
            throw new IllegalArgumentException("All QCM questions must be answered");
        }

        Map<String, QcmAnswerRequest> answerMap = request.getAnswers() == null
                ? Map.of()
                : request.getAnswers().stream()
                        .filter(a -> a.getQuestionId() != null)
                        .collect(Collectors.toMap(QcmAnswerRequest::getQuestionId, a -> a, (a, b) -> a));

        answerRepository.deleteByAssignmentId(assignmentId);
        dimensionScoreRepository.deleteByAssignmentId(assignmentId);

        int score = 0;
        Map<String, List<Double>> dimensionRaw = new LinkedHashMap<>();

        for (QcmQuestion question : questions) {
            QcmAnswerRequest answer = answerMap.get(question.getQuestionId());
            String selected = answer != null ? answer.getSelectedOption() : null;
            boolean correct = !violated
                    && selected != null
                    && question.getCorrectOption().equalsIgnoreCase(selected);
            if (correct) {
                score++;
            }
            answerRepository.save(HiredQcmAnswer.builder()
                    .assignmentId(assignmentId)
                    .questionId(question.getQuestionId())
                    .selectedOption(selected != null ? selected : "-")
                    .correct(correct)
                    .build());

            if (!violated && StringUtils.hasText(question.getDimensionCode()) && selected != null) {
                double optionScore = resolveOptionScore(question, selected);
                dimensionRaw
                        .computeIfAbsent(question.getDimensionCode().trim().toUpperCase(), k -> new ArrayList<>())
                        .add(optionScore);
            }
        }

        Integer overallFit = null;
        if (!violated && !dimensionRaw.isEmpty()) {
            List<HiredQcmDimensionScore> savedScores = new ArrayList<>();
            int order = 0;
            double sum = 0;
            int count = 0;
            for (DimensionDef def : PsychometricDimensions.all()) {
                List<Double> values = dimensionRaw.get(def.code());
                if (values == null || values.isEmpty()) {
                    continue;
                }
                double avg10 = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                double score01 = Math.max(0, Math.min(1, avg10 / 10.0));
                sum += score01;
                count++;
                savedScores.add(HiredQcmDimensionScore.builder()
                        .assignmentId(assignmentId)
                        .dimensionCode(def.code())
                        .dimensionLabel(def.label())
                        .score(round2(score01))
                        .expectedScore(def.expectedScore())
                        .commentText(PsychometricDimensions.commentFor(def, score01))
                        .sortOrder(order++)
                        .build());
            }
            // include unknown dimension codes if any
            for (Map.Entry<String, List<Double>> entry : dimensionRaw.entrySet()) {
                if (PsychometricDimensions.get(entry.getKey()) != null) {
                    continue;
                }
                double avg10 = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
                double score01 = Math.max(0, Math.min(1, avg10 / 10.0));
                sum += score01;
                count++;
                savedScores.add(HiredQcmDimensionScore.builder()
                        .assignmentId(assignmentId)
                        .dimensionCode(entry.getKey())
                        .dimensionLabel(entry.getKey())
                        .score(round2(score01))
                        .expectedScore(0.7)
                        .commentText("Score dimension calculé à partir du QCM")
                        .sortOrder(order++)
                        .build());
            }
            dimensionScoreRepository.saveAll(savedScores);
            if (count > 0) {
                overallFit = (int) Math.round((sum / count) * 100);
            }
        } else if (!violated && questions.size() > 0) {
            overallFit = (int) Math.round((score * 100.0) / questions.size());
        } else {
            overallFit = 0;
        }

        assignment.setScore(violated ? 0 : score);
        assignment.setTotalQuestions(questions.size());
        assignment.setOverallFitPercent(overallFit);
        assignment.setQcmViolated(violated);
        assignment.setStatus(violated ? HiredQcmStatus.VIOLATED : HiredQcmStatus.COMPLETED);
        assignment.setCompletedAt(LocalDateTime.now());
        return toResponse(assignmentRepository.save(assignment), false, true);
    }

    public List<HiredQcmDimensionScore> getDimensionScores(String assignmentId) {
        return dimensionScoreRepository.findByAssignmentIdOrderBySortOrderAsc(assignmentId);
    }

    public UserSummary loadCandidateSummary(String userId) {
        ApiResponse<UserDto> userResp = userClient.getUserById(internalApiKey, userId);
        if (userResp.isSuccess() && userResp.getData() != null) {
            UserDto u = userResp.getData();
            return UserSummary.builder()
                    .userId(u.getUserId())
                    .firstName(u.getFirstName())
                    .lastName(u.getLastName())
                    .username(u.getUsername())
                    .email(u.getEmail())
                    .phoneNumber(u.getPhoneNumber())
                    .profileImageUrl(u.getProfileImageUrl())
                    .build();
        }
        return null;
    }

    public String resolveQcmTitle(String qcmId) {
        return qcmRepository.findByQcmId(qcmId).map(Qcm::getTitle).orElse("Évaluation");
    }

    public String resolveRecruitmentTitle(String recruitmentId) {
        return recruitmentRepository.findByRecruitmentId(recruitmentId)
                .map(Recruitment::getTitle)
                .orElse("");
    }

    public String resolveCompanyName(String recruitmentId) {
        return recruitmentRepository.findByRecruitmentId(recruitmentId)
                .map(r -> companyRepository.findByCompanyId(r.getCompanyId()).map(Company::getName).orElse(""))
                .orElse("");
    }

    private double resolveOptionScore(QcmQuestion question, String selected) {
        String opt = selected.trim().toUpperCase();
        Double configured = switch (opt) {
            case "A" -> question.getScoreA();
            case "B" -> question.getScoreB();
            case "C" -> question.getScoreC();
            case "D" -> question.getScoreD();
            default -> null;
        };
        if (configured != null) {
            return Math.max(0, Math.min(10, configured));
        }
        // Classic QCM fallback: correct = 10, else 0
        if (question.getCorrectOption() != null && question.getCorrectOption().equalsIgnoreCase(opt)) {
            return 10.0;
        }
        return 0.0;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private HiredQcmAssignment getAssignmentOrThrow(String assignmentId) {
        return assignmentRepository.findByAssignmentId(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));
    }

    private void ensureCandidateOwner(HiredQcmAssignment assignment, AuthUser authUser) {
        if (!authUser.isCandidate() || !assignment.getCandidateUserId().equals(authUser.getUserId())) {
            throw new IllegalArgumentException("Access denied");
        }
    }

    private void ensureRhZone(String rhUserId, String zoneId) {
        boolean allowed = rhZoneAssignmentRepository.findByRhUserId(rhUserId).stream()
                .anyMatch(a -> a.getZoneId().equals(zoneId));
        if (!allowed) {
            throw new IllegalArgumentException("Access denied: outside your zone");
        }
    }

    private HiredQcmAssignmentResponse toResponse(
            HiredQcmAssignment assignment,
            boolean includeQuestions,
            boolean includeCandidate) {
        String qcmTitle = resolveQcmTitle(assignment.getQcmId());
        String recruitmentTitle = resolveRecruitmentTitle(assignment.getRecruitmentId());
        String companyName = resolveCompanyName(assignment.getRecruitmentId());

        List<QcmQuestionResponse> questions = null;
        if (includeQuestions) {
            questions = qcmQuestionRepository.findByQcmIdOrderByOrderIndexAsc(assignment.getQcmId()).stream()
                    .map(q -> QcmQuestionResponse.builder()
                            .questionId(q.getQuestionId())
                            .questionText(q.getQuestionText())
                            .optionA(q.getOptionA())
                            .optionB(q.getOptionB())
                            .optionC(q.getOptionC())
                            .optionD(q.getOptionD())
                            .orderIndex(q.getOrderIndex())
                            .dimensionCode(q.getDimensionCode())
                            .build())
                    .toList();
        }

        List<ApplicationAnswerResponse> answers = null;
        if (assignment.getStatus() != HiredQcmStatus.ASSIGNED) {
            answers = answerRepository.findByAssignmentId(assignment.getAssignmentId()).stream()
                    .map(ans -> {
                        QcmQuestion q = qcmQuestionRepository.findByQuestionId(ans.getQuestionId()).orElse(null);
                        return ApplicationAnswerResponse.builder()
                                .questionId(ans.getQuestionId())
                                .questionText(q != null ? q.getQuestionText() : "")
                                .selectedOption(ans.getSelectedOption())
                                .correct(ans.isCorrect())
                                .build();
                    })
                    .toList();
        }

        List<DimensionScoreResponse> dimensionScores = null;
        if (assignment.getStatus() != HiredQcmStatus.ASSIGNED) {
            dimensionScores = dimensionScoreRepository
                    .findByAssignmentIdOrderBySortOrderAsc(assignment.getAssignmentId())
                    .stream()
                    .map(d -> DimensionScoreResponse.builder()
                            .dimensionCode(d.getDimensionCode())
                            .dimensionLabel(d.getDimensionLabel())
                            .score(d.getScore())
                            .expectedScore(d.getExpectedScore())
                            .commentText(d.getCommentText())
                            .sortOrder(d.getSortOrder())
                            .build())
                    .toList();
        }

        UserSummary candidate = includeCandidate ? loadCandidateSummary(assignment.getCandidateUserId()) : null;

        return HiredQcmAssignmentResponse.builder()
                .assignmentId(assignment.getAssignmentId())
                .applicationId(assignment.getApplicationId())
                .candidateUserId(assignment.getCandidateUserId())
                .recruitmentId(assignment.getRecruitmentId())
                .recruitmentTitle(recruitmentTitle)
                .companyName(companyName)
                .qcmId(assignment.getQcmId())
                .qcmTitle(qcmTitle)
                .status(assignment.getStatus().name())
                .score(assignment.getScore())
                .totalQuestions(assignment.getTotalQuestions())
                .overallFitPercent(assignment.getOverallFitPercent())
                .qcmViolated(assignment.getQcmViolated())
                .assignedAt(assignment.getAssignedAt())
                .completedAt(assignment.getCompletedAt())
                .candidate(candidate)
                .questions(questions)
                .answers(answers)
                .dimensionScores(dimensionScores)
                .build();
    }
}
