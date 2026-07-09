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
            LocalDateTime interviewAt,
            String meetLink) {
        sendToRecipient(candidateEmail, candidateName, jobTitle, region, interviewAt, meetLink, false);
        sendToRecipient(rhEmail, rhName, jobTitle, region, interviewAt, meetLink, true);
    }

    private void sendToRecipient(
            String toEmail,
            String recipientName,
            String jobTitle,
            String region,
            LocalDateTime interviewAt,
            String meetLink,
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
                : "Candidature acceptee — entretien " + jobTitle;

        String greeting = StringUtils.hasText(recipientName) ? recipientName : (isRh ? "RH" : "Candidat");
        String intro = isRh
                ? "Vous avez planifie un entretien pour le poste <strong>" + escapeHtml(jobTitle) + "</strong>."
                : "Votre candidature pour le poste <strong>" + escapeHtml(jobTitle) + "</strong> a ete <strong>acceptee</strong>.";

        String htmlBody = """
                <html>
                <body style="font-family: Arial, sans-serif; color: #222;">
                  <p>Bonjour %s,</p>
                  <p>%s</p>
                  <p><strong>Details de l'entretien :</strong></p>
                  <ul>
                    <li><strong>Date :</strong> %s</li>
                    <li><strong>Region :</strong> %s</li>
                    <li><strong>Google Meet :</strong> <a href="%s">%s</a></li>
                  </ul>
                  <p>Merci de rejoindre la reunion a l'heure indiquee.</p>
                  <p>Cordialement,<br/>L'equipe RH DAAM</p>
                </body>
                </html>
                """.formatted(
                escapeHtml(greeting),
                intro,
                interviewAt.format(DATE_FORMAT),
                escapeHtml(StringUtils.hasText(region) ? region : "Non precisee"),
                meetLink,
                escapeHtml(meetLink));

        String textBody = """
                Bonjour %s,

                %s

                Details de l'entretien :
                - Date : %s
                - Region : %s
                - Lien Google Meet : %s

                Merci de rejoindre la reunion a l'heure indiquee.

                Cordialement,
                L'equipe RH DAAM
                """.formatted(
                greeting,
                isRh ? "Vous avez planifie un entretien pour le poste " + jobTitle + "."
                        : "Votre candidature pour le poste " + jobTitle + " a ete acceptee.",
                interviewAt.format(DATE_FORMAT),
                StringUtils.hasText(region) ? region : "Non precisee",
                meetLink);

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
