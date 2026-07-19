package com.daam.recruitment.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewEmailService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter PHYSICAL_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter PHYSICAL_TIME_FORMAT =
            DateTimeFormatter.ofPattern("H'h'mm", Locale.FRENCH);
    private static final DateTimeFormatter HIRE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final String HIRE_DOC_PATH = "hire-docs/liste-documentation-dossier-employe.docx";
    private static final String HIRE_DOC_FILENAME = "Liste documentation dossier employe.docx";

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

    /**
     * Formal in-person interview invitation matching the DAAM convocation template.
     */
    public void sendPhysicalInterviewInvitation(
            String candidateEmail,
            String candidateName,
            String rhEmail,
            String rhName,
            String jobTitle,
            String companyName,
            String companyAddress,
            String interviewLocation,
            LocalDateTime interviewStart) {
        sendPhysicalToCandidate(candidateEmail, candidateName, jobTitle, companyName,
                companyAddress, interviewLocation, interviewStart, rhName);
        sendPhysicalRhCopy(rhEmail, rhName, candidateName, jobTitle, companyName,
                interviewLocation, interviewStart);
    }

    private void sendPhysicalToCandidate(
            String toEmail,
            String candidateName,
            String jobTitle,
            String companyName,
            String companyAddress,
            String interviewLocation,
            LocalDateTime interviewStart,
            String interlocutorName) {
        if (!StringUtils.hasText(toEmail)) {
            log.warn("Physical interview email skipped: no candidate address");
            return;
        }
        if (!mailEnabled) {
            log.warn("Physical interview email skipped: app.mail.enabled=false");
            return;
        }

        String agency = StringUtils.hasText(companyName) ? companyName.trim() : "l'agence";
        String addressLine = StringUtils.hasText(companyAddress) ? companyAddress.trim() : "adresse a confirmer";
        String lieu = StringUtils.hasText(interviewLocation) ? interviewLocation.trim() : addressLine;
        String dateFr = formatPhysicalDate(interviewStart);
        String timeFr = formatPhysicalTime(interviewStart);
        String interlocutor = formatInterlocutor(interlocutorName);
        String greeting = StringUtils.hasText(candidateName) ? candidateName.trim() : "Candidat";

        String subject = "Convocation à un entretien – Poste de " + jobTitle + " | " + agency;

        String htmlBody = """
                <html>
                <body style="font-family: Arial, sans-serif; color: #222; line-height: 1.6;">
                  <p>Bonjour %s,</p>
                  <p>À la suite de notre échange téléphonique, nous avons le plaisir de vous confirmer
                  votre convocation à un entretien de recrutement dans le cadre de l’étude de votre
                  candidature au poste de <strong>%s</strong> au sein de l’agence
                  <strong>%s</strong>, située au %s.</p>
                  <p>L’entretien se déroulera selon les modalités suivantes :</p>
                  <p>
                    <strong>Date :</strong> %s<br/>
                    <strong>Heure :</strong> %s<br/>
                    <strong>Lieu :</strong> %s
                  </p>
                  <p><strong>Interlocuteur :</strong> %s.</p>
                  <p>Nous vous remercions de bien vouloir confirmer votre présence par retour d’e-mail
                  dans les meilleurs délais.</p>
                  <p>Cordialement,</p>
                </body>
                </html>
                """.formatted(
                escapeHtml(greeting),
                escapeHtml(jobTitle),
                escapeHtml(agency),
                escapeHtml(addressLine),
                escapeHtml(dateFr),
                escapeHtml(timeFr),
                escapeHtml(lieu),
                escapeHtml(interlocutor));

        String textBody = """
                Bonjour %s,

                À la suite de notre échange téléphonique, nous avons le plaisir de vous confirmer votre convocation à un entretien de recrutement dans le cadre de l’étude de votre candidature au poste de %s au sein de l’agence %s, située au %s.

                L’entretien se déroulera selon les modalités suivantes :

                Date : %s
                Heure : %s
                Lieu : %s

                Interlocuteur : %s.

                Nous vous remercions de bien vouloir confirmer votre présence par retour d’e-mail dans les meilleurs délais.

                Cordialement,
                """.formatted(
                greeting, jobTitle, agency, addressLine, dateFr, timeFr, lieu, interlocutor);

        sendMail(toEmail, subject, textBody, htmlBody, "candidate-physical", false);
    }

    private void sendPhysicalRhCopy(
            String toEmail,
            String rhName,
            String candidateName,
            String jobTitle,
            String companyName,
            String interviewLocation,
            LocalDateTime interviewStart) {
        if (!StringUtils.hasText(toEmail) || !mailEnabled) {
            return;
        }
        String subject = "Entretien physique planifie — " + jobTitle;
        String dateFr = formatPhysicalDate(interviewStart);
        String timeFr = formatPhysicalTime(interviewStart);
        String lieu = StringUtils.hasText(interviewLocation) ? interviewLocation : "Non precise";
        String greeting = StringUtils.hasText(rhName) ? rhName : "RH";

        String htmlBody = """
                <html><body style="font-family: Arial, sans-serif; color: #222;">
                  <p>Bonjour %s,</p>
                  <p>Convocation physique envoyee au candidat <strong>%s</strong>
                  pour le poste <strong>%s</strong> (%s).</p>
                  <ul>
                    <li><strong>Date :</strong> %s</li>
                    <li><strong>Heure :</strong> %s</li>
                    <li><strong>Lieu :</strong> %s</li>
                  </ul>
                </body></html>
                """.formatted(
                escapeHtml(greeting),
                escapeHtml(candidateName != null ? candidateName : ""),
                escapeHtml(jobTitle),
                escapeHtml(companyName != null ? companyName : ""),
                escapeHtml(dateFr),
                escapeHtml(timeFr),
                escapeHtml(lieu));

        String textBody = "Bonjour " + greeting + ",\n\nConvocation physique envoyee au candidat "
                + candidateName + " pour " + jobTitle + ".\nDate : " + dateFr + "\nHeure : " + timeFr
                + "\nLieu : " + lieu + "\n";

        sendMail(toEmail, subject, textBody, htmlBody, "rh-physical", false);
    }

    /**
     * Formal hiring confirmation email with documentation Word attachment.
     */
    public void sendHireConfirmation(
            String candidateEmail,
            String candidateName,
            String rhEmail,
            String rhName,
            String jobTitle,
            String companyName,
            LocalDate startDate,
            String contractType,
            String workingHours,
            String netSalary,
            String benefits,
            String integrationAddress,
            String integrationGpsUrl) {
        sendHireToCandidate(candidateEmail, candidateName, jobTitle, companyName, startDate,
                contractType, workingHours, netSalary, benefits, integrationAddress, integrationGpsUrl);
        sendHireRhCopy(rhEmail, rhName, candidateName, jobTitle, companyName, startDate, netSalary);
    }

    private void sendHireToCandidate(
            String toEmail,
            String candidateName,
            String jobTitle,
            String companyName,
            LocalDate startDate,
            String contractType,
            String workingHours,
            String netSalary,
            String benefits,
            String integrationAddress,
            String integrationGpsUrl) {
        if (!StringUtils.hasText(toEmail)) {
            log.warn("Hire confirmation email skipped: no candidate address");
            return;
        }
        if (!mailEnabled) {
            log.warn("Hire confirmation email skipped: app.mail.enabled=false");
            return;
        }

        String agency = StringUtils.hasText(companyName) ? companyName.trim() : "DAAM";
        String poste = StringUtils.hasText(jobTitle) ? jobTitle.trim() : "Poste";
        String dateFr = formatHireDate(startDate);
        String greeting = StringUtils.hasText(candidateName) ? candidateName.trim() : "Madame / Monsieur";
        String address = StringUtils.hasText(integrationAddress) ? integrationAddress.trim() : "Adresse à confirmer";
        String gps = StringUtils.hasText(integrationGpsUrl) ? integrationGpsUrl.trim() : "";
        String benefitsHtml = toHtmlLines(benefits);
        String benefitsText = benefits != null ? benefits : "";

        String subject = "CONFIRMATION D'EMBAUCHE / Validation de votre candidature– "
                + poste + " | " + agency + ".";

        String gpsHtml = StringUtils.hasText(gps)
                ? "<p>Accès GPS : <a href=\"" + escapeHtml(gps) + "\">" + escapeHtml(gps) + "</a></p>"
                : "";
        String gpsText = StringUtils.hasText(gps) ? "Accès GPS : " + gps + "\n" : "";

        String htmlBody = """
                <html>
                <body style="font-family: Arial, sans-serif; color: #222; line-height: 1.6;">
                  <p>Madame / Monsieur%s,</p>
                  <p>À la suite des différents entretiens réalisés dans le cadre de notre processus de recrutement,
                  nous avons le plaisir de vous informer que votre candidature au poste de
                  <strong>%s</strong> a été retenue.</p>
                  <p>Nous vous remercions de l’intérêt porté à DAAM et vous souhaitons la bienvenue au sein de nos équipes.</p>
                  <p>Les conditions relatives à votre recrutement sont les suivantes :</p>
                  <p>
                    <strong>Intitulé du poste :</strong> %s – %s.<br/>
                    <strong>Nature du contrat :</strong> %s<br/>
                    <strong>Date de prise de fonction :</strong> %s<br/>
                    <strong>Horaires de travail :</strong> %s<br/>
                    <strong>Rémunération nette mensuelle :</strong> %s
                  </p>
                  <p><strong>Avantages accordés :</strong></p>
                  %s
                  <p>Votre intégration est prévue le <strong>%s</strong> à 08h00 au siège social / agence,
                  à l’adresse suivante :</p>
                  <p>%s</p>
                  %s
                  <p>Afin de finaliser votre dossier administratif, nous vous remercions de bien vouloir :</p>
                  <ul>
                    <li>Confirmer par retour d’e-mail votre acceptation de la présente offre ;</li>
                    <li>Confirmer votre disponibilité à la date de prise de fonction mentionnée ci-dessus ;</li>
                    <li>Transmettre une copie de votre carte d’identité nationale dans les meilleurs délais ;</li>
                    <li>Présenter l’ensemble des documents requis le jour de votre intégration,
                    conformément à la liste jointe au présent courrier.</li>
                  </ul>
                  <p>Par ailleurs, un lien d’évaluation en ligne vous sera transmis prochainement via la
                  plateforme KEEJOB EVALUATION. Nous vous invitons à compléter ce test dans les délais
                  qui vous seront communiqués.</p>
                  <p>Nous restons à votre entière disposition pour toute information complémentaire et nous
                  réjouissons de vous compter prochainement parmi nos collaborateurs.</p>
                  <p>Cordialement,</p>
                </body>
                </html>
                """.formatted(
                StringUtils.hasText(candidateName) ? " " + escapeHtml(greeting) : "",
                escapeHtml(poste),
                escapeHtml(poste),
                escapeHtml(agency),
                escapeHtml(contractType),
                escapeHtml(dateFr),
                escapeHtml(workingHours),
                escapeHtml(netSalary),
                benefitsHtml,
                escapeHtml(dateFr),
                escapeHtml(address),
                gpsHtml);

        String textBody = """
                Madame / Monsieur,

                À la suite des différents entretiens réalisés dans le cadre de notre processus de recrutement, nous avons le plaisir de vous informer que votre candidature au poste de %s a été retenue.

                Nous vous remercions de l’intérêt porté à DAAM et vous souhaitons la bienvenue au sein de nos équipes.

                Les conditions relatives à votre recrutement sont les suivantes :

                Intitulé du poste : %s – %s.
                Nature du contrat : %s
                Date de prise de fonction : %s
                Horaires de travail : %s
                Rémunération nette mensuelle : %s

                Avantages accordés :
                %s

                Votre intégration est prévue le %s à 08h00 à l’adresse suivante :
                %s
                %s
                Afin de finaliser votre dossier administratif, veuillez confirmer votre acceptation par retour d’e-mail, confirmer votre disponibilité, transmettre une copie de votre CIN, et présenter les documents requis (liste jointe).

                Cordialement,
                """.formatted(
                poste, poste, agency, contractType, dateFr, workingHours, netSalary,
                benefitsText, dateFr, address, gpsText);

        sendMail(toEmail, subject, textBody, htmlBody, "candidate-hire", true);
    }

    private void sendHireRhCopy(
            String toEmail,
            String rhName,
            String candidateName,
            String jobTitle,
            String companyName,
            LocalDate startDate,
            String netSalary) {
        if (!StringUtils.hasText(toEmail) || !mailEnabled) {
            return;
        }
        String subject = "Confirmation d'embauche envoyée — " + jobTitle;
        String dateFr = formatHireDate(startDate);
        String greeting = StringUtils.hasText(rhName) ? rhName : "RH";
        String htmlBody = """
                <html><body style="font-family: Arial, sans-serif; color: #222;">
                  <p>Bonjour %s,</p>
                  <p>La confirmation d'embauche a été envoyée au candidat <strong>%s</strong>
                  pour le poste <strong>%s</strong> (%s).</p>
                  <ul>
                    <li><strong>Date de prise de fonction :</strong> %s</li>
                    <li><strong>Rémunération :</strong> %s</li>
                  </ul>
                  <p>La liste de documentation a été jointe à l'e-mail du candidat.</p>
                </body></html>
                """.formatted(
                escapeHtml(greeting),
                escapeHtml(candidateName != null ? candidateName : ""),
                escapeHtml(jobTitle),
                escapeHtml(companyName != null ? companyName : ""),
                escapeHtml(dateFr),
                escapeHtml(netSalary));
        String textBody = "Confirmation d'embauche envoyée à " + candidateName + " pour " + jobTitle
                + ". Date : " + dateFr + ". Rémunération : " + netSalary;
        sendMail(toEmail, subject, textBody, htmlBody, "rh-hire", false);
    }

    private String formatHireDate(LocalDate date) {
        if (date == null) {
            return "À confirmer";
        }
        return capitalizeWords(date.format(HIRE_DATE_FORMAT));
    }

    private String toHtmlLines(String benefits) {
        if (!StringUtils.hasText(benefits)) {
            return "<p>Non précisé</p>";
        }
        StringBuilder sb = new StringBuilder("<ul>");
        for (String line : benefits.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                sb.append("<li>").append(escapeHtml(trimmed)).append("</li>");
            }
        }
        sb.append("</ul>");
        return sb.toString();
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

        sendMail(toEmail, subject, textBody, htmlBody, isRh ? "rh" : "candidate", false);
    }

    private void sendMail(
            String toEmail,
            String subject,
            String textBody,
            String htmlBody,
            String kind,
            boolean attachHireDoc) {
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
            if (attachHireDoc) {
                ClassPathResource doc = new ClassPathResource(HIRE_DOC_PATH);
                if (doc.exists()) {
                    helper.addAttachment(HIRE_DOC_FILENAME, doc);
                } else {
                    log.warn("Hire documentation file missing on classpath: {}", HIRE_DOC_PATH);
                }
            }
            mailSender.send(message);
            log.info("Interview email sent to {} ({})", toEmail, kind);
        } catch (Exception e) {
            log.error("Failed to send interview email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private String formatPhysicalDate(LocalDateTime start) {
        if (start == null) {
            return "A confirmer";
        }
        String raw = start.format(PHYSICAL_DATE_FORMAT);
        return capitalizeWords(raw);
    }

    private String formatPhysicalTime(LocalDateTime start) {
        if (start == null) {
            return "A confirmer";
        }
        return start.format(PHYSICAL_TIME_FORMAT);
    }

    private String formatInterlocutor(String rhName) {
        if (!StringUtils.hasText(rhName)) {
            return "le service RH";
        }
        String name = rhName.trim();
        if (name.regionMatches(true, 0, "M.", 0, 2)
                || name.regionMatches(true, 0, "Mme", 0, 3)
                || name.regionMatches(true, 0, "Mlle", 0, 4)) {
            return name;
        }
        return "M. " + name;
    }

    private String capitalizeWords(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        StringBuilder sb = new StringBuilder();
        for (String part : value.split(" ")) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
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
