package com.daam.recruitment.service;

import com.daam.recruitment.dto.RecruitmentDtos.QcmQuestionRequest;
import com.daam.recruitment.dto.RecruitmentDtos.QcmQuestionResponse;
import com.daam.recruitment.dto.RecruitmentDtos.QcmRequest;
import com.daam.recruitment.dto.RecruitmentDtos.QcmResponse;
import com.daam.recruitment.entity.Qcm;
import com.daam.recruitment.entity.QcmQuestion;
import com.daam.recruitment.entity.Recruitment;
import com.daam.recruitment.repository.QcmQuestionRepository;
import com.daam.recruitment.repository.QcmRepository;
import com.daam.recruitment.repository.RecruitmentRepository;
import com.daam.recruitment.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QcmService {

    private final QcmRepository qcmRepository;
    private final QcmQuestionRepository qcmQuestionRepository;
    private final RecruitmentRepository recruitmentRepository;

    @Transactional
    public QcmResponse createQcm(QcmRequest request, AuthUser authUser) {
        validateQuestions(request.getQuestions());
        Qcm qcm = Qcm.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .createdByRhUserId(authUser.getUserId())
                .build();
        Qcm saved = qcmRepository.save(qcm);
        saveQuestions(saved.getQcmId(), request.getQuestions());
        return toResponse(saved, true);
    }

    @Transactional
    public QcmResponse updateQcm(String qcmId, QcmRequest request, AuthUser authUser) {
        Qcm qcm = getQcmOrThrow(qcmId);
        ensureCanEdit(qcm, authUser);
        validateQuestions(request.getQuestions());

        qcm.setTitle(request.getTitle().trim());
        qcm.setDescription(request.getDescription());
        Qcm saved = qcmRepository.save(qcm);

        qcmQuestionRepository.deleteByQcmId(qcmId);
        saveQuestions(qcmId, request.getQuestions());
        return toResponse(saved, true);
    }

    @Transactional(readOnly = true)
    public List<QcmResponse> listQcms(AuthUser authUser) {
        List<Qcm> qcms = authUser.isAdmin()
                ? qcmRepository.findAllByOrderByCreatedAtDesc()
                : qcmRepository.findAllByOrderByCreatedAtDesc();
        return qcms.stream().map(q -> toResponse(q, false)).toList();
    }

    @Transactional(readOnly = true)
    public QcmResponse getQcm(String qcmId, boolean includeCorrect) {
        return toResponse(getQcmOrThrow(qcmId), includeCorrect);
    }

    @Transactional
    public void deleteQcm(String qcmId, AuthUser authUser) {
        Qcm qcm = getQcmOrThrow(qcmId);
        ensureCanEdit(qcm, authUser);

        List<Recruitment> linked = recruitmentRepository.findByQcmId(qcmId);
        for (Recruitment recruitment : linked) {
            recruitment.setQcmId(null);
            recruitmentRepository.save(recruitment);
        }

        qcmQuestionRepository.deleteByQcmId(qcmId);
        qcmRepository.delete(qcm);
    }

    public Qcm getQcmOrThrow(String qcmId) {
        return qcmRepository.findByQcmId(qcmId)
                .orElseThrow(() -> new IllegalArgumentException("QCM not found"));
    }

    public List<QcmQuestion> getQuestionsForQcm(String qcmId) {
        return qcmQuestionRepository.findByQcmIdOrderByOrderIndexAsc(qcmId);
    }

    private void ensureCanEdit(Qcm qcm, AuthUser authUser) {
        if (authUser.isAdmin()) {
            return;
        }
        if (!authUser.isRh()) {
            throw new IllegalArgumentException("Access denied");
        }
        if (!qcm.getCreatedByRhUserId().equals(authUser.getUserId())) {
            throw new IllegalArgumentException("You can only edit your own QCM");
        }
    }

    private void validateQuestions(List<QcmQuestionRequest> questions) {
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("At least one QCM question is required");
        }
        for (QcmQuestionRequest q : questions) {
            if (!StringUtils.hasText(q.getQuestionText())
                    || !StringUtils.hasText(q.getOptionA())
                    || !StringUtils.hasText(q.getOptionB())
                    || !StringUtils.hasText(q.getCorrectOption())) {
                throw new IllegalArgumentException("Invalid QCM question");
            }
        }
    }

    private void saveQuestions(String qcmId, List<QcmQuestionRequest> questions) {
        int index = 0;
        for (QcmQuestionRequest q : questions) {
            qcmQuestionRepository.save(QcmQuestion.builder()
                    .qcmId(qcmId)
                    .questionText(q.getQuestionText())
                    .optionA(q.getOptionA())
                    .optionB(q.getOptionB())
                    .optionC(q.getOptionC())
                    .optionD(q.getOptionD())
                    .correctOption(q.getCorrectOption().toUpperCase())
                    .orderIndex(q.getOrderIndex() != null ? q.getOrderIndex() : index++)
                    .build());
        }
    }

    private QcmResponse toResponse(Qcm qcm, boolean includeQuestions) {
        List<QcmQuestion> questions = qcmQuestionRepository.findByQcmIdOrderByOrderIndexAsc(qcm.getQcmId());
        List<QcmQuestionResponse> questionResponses = includeQuestions
                ? questions.stream().map(this::toQuestionResponse).toList()
                : null;
        return QcmResponse.builder()
                .qcmId(qcm.getQcmId())
                .title(qcm.getTitle())
                .description(qcm.getDescription())
                .createdByRhUserId(qcm.getCreatedByRhUserId())
                .createdAt(qcm.getCreatedAt())
                .questionCount(questions.size())
                .questions(questionResponses)
                .build();
    }

    private QcmQuestionResponse toQuestionResponse(QcmQuestion q) {
        return QcmQuestionResponse.builder()
                .questionId(q.getQuestionId())
                .questionText(q.getQuestionText())
                .optionA(q.getOptionA())
                .optionB(q.getOptionB())
                .optionC(q.getOptionC())
                .optionD(q.getOptionD())
                .correctOption(q.getCorrectOption())
                .orderIndex(q.getOrderIndex())
                .build();
    }
}
