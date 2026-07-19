package com.daam.recruitment.service;

import com.daam.recruitment.dto.InterviewScheduleResult;
import com.daam.recruitment.entity.Interview;
import com.daam.recruitment.entity.JobApplication;
import com.daam.recruitment.entity.Recruitment;
import com.daam.recruitment.enumeration.InterviewStatus;
import com.daam.recruitment.enumeration.MeetingProviderType;
import com.daam.recruitment.repository.InterviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;

    @Value("${app.interview.duration-minutes:60}")
    private int defaultDurationMinutes;

    @Transactional
    public InterviewScheduleResult scheduleInterview(
            JobApplication application,
            Recruitment recruitment,
            String recruiterUserId,
            String candidateName,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            String rhMeetingLink) {
        return scheduleInterview(application, recruitment, recruiterUserId, candidateName,
                startDateTime, endDateTime, rhMeetingLink, false);
    }

    @Transactional
    public InterviewScheduleResult scheduleInterview(
            JobApplication application,
            Recruitment recruitment,
            String recruiterUserId,
            String candidateName,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            String rhMeetingLink,
            boolean physical) {

        if (startDateTime == null) {
            throw new IllegalArgumentException("Interview start date/time is required");
        }
        LocalDateTime end = endDateTime != null
                ? endDateTime
                : startDateTime.plusMinutes(Math.max(15, defaultDurationMinutes));
        if (!end.isAfter(startDateTime)) {
            throw new IllegalArgumentException("Interview end time must be after start time");
        }

        interviewRepository.findByApplicationIdAndStatus(application.getApplicationId(), InterviewStatus.SCHEDULED)
                .ifPresent(existing -> {
                    existing.setStatus(InterviewStatus.CANCELLED);
                    interviewRepository.save(existing);
                });

        String title = buildTitle(recruitment.getTitle(), candidateName);
        String meetingLink = null;
        String warning = null;
        MeetingProviderType provider;

        if (physical) {
            provider = MeetingProviderType.PHYSICAL;
        } else {
            meetingLink = StringUtils.hasText(rhMeetingLink) ? rhMeetingLink.trim() : null;
            warning = meetingLink == null
                    ? "Entretien enregistre. Ajoutez votre lien de reunion (Teams, Meet, etc.) dans Mon profil."
                    : null;
            provider = meetingLink != null ? MeetingProviderType.MANUAL : MeetingProviderType.NONE;
        }

        Interview interview = Interview.builder()
                .applicationId(application.getApplicationId())
                .recruitmentId(recruitment.getRecruitmentId())
                .recruiterUserId(recruiterUserId)
                .candidateUserId(application.getCandidateUserId())
                .title(title)
                .startDateTime(startDateTime)
                .endDateTime(end)
                .status(InterviewStatus.SCHEDULED)
                .meetingProvider(provider)
                .meetingLink(meetingLink)
                .meetingWarning(warning)
                .build();

        Interview saved = interviewRepository.save(interview);

        application.setInterviewAt(startDateTime);
        application.setInterviewEndAt(end);
        application.setGoogleMeetLink(meetingLink);
        application.setMeetingProvider(provider.name());
        application.setMeetingId(null);
        application.setMeetingWarning(warning);

        return InterviewScheduleResult.builder()
                .interview(saved)
                .meetingLink(meetingLink)
                .meetingProvider(provider.name())
                .warningMessage(warning)
                .build();
    }

    @Transactional
    public void cancelByApplication(String applicationId) {
        interviewRepository.findByApplicationIdAndStatus(applicationId, InterviewStatus.SCHEDULED)
                .ifPresent(interview -> {
                    interview.setStatus(InterviewStatus.CANCELLED);
                    interviewRepository.save(interview);
                });
    }

    private String buildTitle(String recruitmentTitle, String candidateName) {
        String job = StringUtils.hasText(recruitmentTitle) ? recruitmentTitle.trim() : "Recruitment";
        String candidate = StringUtils.hasText(candidateName) ? candidateName.trim() : "Candidate";
        return "Interview - " + job + " - " + candidate;
    }
}
