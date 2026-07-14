package com.daam.recruitment.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewEmailService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:daam-rh@example.com}")
    private String fromAddress;

    public void sendInterviewNotifications(
            String candidateEmail,
            String candidateName,
            String rhEmail,
            String rhName,
            String jobTitle,
            String region,
            LocalDateTime interviewStart,
            LocalDateTime interviewEnd,
            String meetingLink,
            String meetingProvider,
            String warningMessage) {
        sendToRecipient(candidateEmail, candidateName, jobTitle, region, interviewStart, interviewEnd,
                meetingLink, meetingProvider, warningMessage, false);
        sendToRecipient(rhEmail, rhName, jobTitle, region, interviewStart, interviewEnd,
                meetingLink, meetingProvider, warningMessage, true);
    }

    private void sendToRecipient(
            String toEmail,
            String recipientName,
            String jobTitle,
            String region,
            LocalDateTime interviewStart,
            LocalDateTime interviewEnd,
            String meetingLink,
            String meetingProvider,
            String warningMessage,
            boolean isRh) {
        if (!StringUtils.hasText(toEmail)) {
            log.warn("Interview email skipped: no address for {}", isRh ? "RH" : "candidate");
            return;
        }
        if (!mailEnabled) {
            log.warn("Interview email skipped: app.mail.enabled=false");
            return;
        }

        String subject = isRh
                ? "Entretien planifie — " + jobTitle
                : "Invitation entretien — " + jobTitle;

        String greeting = StringUtils.hasText(recipientName) ? recipientName : (isRh ? "RH" : "Candidat");
        String intro = isRh
                ? "Vous avez planifie un entretien pour le poste <strong>" + escapeHtml(jobTitle) + "</strong>."
                : "Votre candidature pour le poste <strong>" + escapeHtml(jobTitle) + "</strong> a ete <strong>acceptee</strong>.";

        String timeRange = formatTimeRange(interviewStart, interviewEnd);
        String meetingSection = buildMeetingSection(meetingLink, meetingProvider);
        String warningSection = StringUtils.hasText(warningMessage) && isRh
                ? "<p style=\"color:#b45309;\"><strong>Note :</strong> " + escapeHtml(warningMessage) + "</p>"
                : "";

        String htmlBody = """
                <html>
                <body style="font-family: Arial, sans-serif; color: #222;">
                  <p>Bonjour %s,</p>
                  <p>%s</p>
                  <p><strong>Details de l'entretien :</strong></p>
                  <ul>
                    <li><strong>Date :</strong> %s</li>
                    <li><strong>Region :</strong> %s</li>
                    %s
                  </ul>
                  %s
                  <p>Merci de rejoindre la reunion a l'heure indiquee.</p>
                  <p>Cordialement,<br/>L'equipe RH DAAM</p>
                </body>
                </html>
                """.formatted(
                escapeHtml(greeting),
                intro,
                timeRange,
                escapeHtml(StringUtils.hasText(region) ? region : "Non precisee"),
                meetingSection,
                warningSection);

        String textMeeting = StringUtils.hasText(meetingLink)
                ? "- Lien " + meetingLabel(meetingProvider) + " : " + meetingLink
                : "- Lien visioconference : sera communique par le RH";

        String textBody = """
                Bonjour %s,

                %s

                Details de l'entretien :
                - Date : %s
                - Region : %s
                %s

                Merci de rejoindre la reunion a l'heure indiquee.

                Cordialement,
                L'equipe RH DAAM
                """.formatted(
                greeting,
                isRh ? "Vous avez planifie un entretien pour le poste " + jobTitle + "."
                        : "Votre candidature pour le poste " + jobTitle + " a ete acceptee.",
                timeRange,
                StringUtils.hasText(region) ? region : "Non precisee",
                textMeeting);

        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) {
                log.error("JavaMailSender not available — email not sent to {}", toEmail);
                return;
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(textBody, htmlBody);
            mailSender.send(message);
            log.info("Interview email sent to {} ({})", toEmail, isRh ? "RH" : "candidate");
        } catch (Exception e) {
            log.error("Failed to send interview email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private String formatTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            return "Non precisee";
        }
        if (end != null && end.isAfter(start)) {
            return start.format(DATE_FORMAT) + " — " + end.format(DATE_FORMAT);
        }
        return start.format(DATE_FORMAT);
    }

    private String buildMeetingSection(String meetingLink, String meetingProvider) {
        if (!StringUtils.hasText(meetingLink)) {
            return "<li><strong>Visioconference :</strong> lien a confirmer</li>";
        }
        String label = meetingLabel(meetingProvider);
        return "<li><strong>" + escapeHtml(label) + " :</strong> "
                + "<a href=\"" + escapeHtml(meetingLink) + "\">" + escapeHtml(meetingLink) + "</a></li>";
    }

    private String meetingLabel(String meetingProvider) {
        if ("MANUAL".equalsIgnoreCase(meetingProvider)) {
            return "Lien de reunion";
        }
        return "Lien de reunion";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
