package com.daam.recruitment.dto;

import com.daam.recruitment.entity.Interview;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InterviewScheduleResult {
    private final Interview interview;
    private final String meetingLink;
    private final String meetingId;
    private final String meetingProvider;
    /** Non-null when interview was saved but Teams link could not be created */
    private final String warningMessage;
}
