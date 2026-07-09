package com.daam.recruitment.service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.ConferenceSolutionKey;
import com.google.api.services.calendar.model.CreateConferenceRequest;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class GoogleMeetService {

    private static final String APPLICATION_NAME = "DAAM Recruitment";
    private static final String DEFAULT_CREDENTIALS = "credentials/google-calendar.json";

    @Value("${google.calendar.enabled:true}")
    private boolean calendarEnabled;

    @Value("${google.calendar.credentials-path:credentials/google-calendar.json}")
    private String credentialsPath;

    @Value("${google.calendar.delegate-user:}")
    private String delegateUser;

    @Value("${google.calendar.timezone:Africa/Casablanca}")
    private String timezone;

    @Value("${app.interview.duration-minutes:60}")
    private int durationMinutes;

    @Value("${app.google-meet.room-url:}")
    private String configuredRoomUrl;

    private Path resolvedCredentialsFile;
    private Resource resolvedCredentialsResource;

    @PostConstruct
    void init() {
        resolveCredentials();
        if (resolvedCredentialsFile != null) {
            log.info("Google Calendar credentials found at {}", resolvedCredentialsFile);
        } else if (resolvedCredentialsResource != null) {
            log.info("Google Calendar credentials loaded from classpath: {}", credentialsPath);
        } else if (calendarEnabled) {
            log.warn("Google Calendar enabled but credentials not found (checked file paths and classpath)");
        }
    }

    public String createMeetLink(
            String title,
            LocalDateTime interviewAt,
            String candidateEmail,
            String candidateName,
            String rhEmail) {
        if (!calendarEnabled) {
            return fallbackMeetLink();
        }

        if (resolvedCredentialsFile == null && resolvedCredentialsResource == null) {
            resolveCredentials();
        }

        if (resolvedCredentialsFile != null || resolvedCredentialsResource != null) {
            try {
                return createViaCalendarApi(title, interviewAt, candidateName);
            } catch (Exception e) {
                log.error("Google Calendar API failed", e);
                throw new IllegalStateException(
                        "Impossible de creer le Google Meet pour le " + interviewAt + ": " + extractMessage(e));
            }
        }

        return fallbackMeetLink();
    }

    private String fallbackMeetLink() {
        if (StringUtils.hasText(configuredRoomUrl)) {
            log.info("Using configured Google Meet room URL");
            return configuredRoomUrl.trim();
        }

        throw new IllegalStateException(
                "Ajoutez un lien Google Meet reel dans application-local.properties : "
                        + "app.google-meet.room-url=https://meet.google.com/xxx-yyyy-zzz "
                        + "(creez une reunion sur https://meet.google.com et copiez le lien).");
    }

    private String extractMessage(Exception e) {
        if (e instanceof GoogleJsonResponseException googleError && googleError.getDetails() != null) {
            return googleError.getDetails().getMessage();
        }
        return e.getMessage();
    }

    private void resolveCredentials() {
        resolvedCredentialsFile = null;
        resolvedCredentialsResource = null;

        if (!StringUtils.hasText(credentialsPath)) {
            return;
        }

        for (Path candidate : buildCredentialCandidates(credentialsPath)) {
            if (Files.isRegularFile(candidate)) {
                resolvedCredentialsFile = candidate.toAbsolutePath();
                return;
            }
        }

        try {
            Resource classpathResource = new ClassPathResource(credentialsPath);
            if (classpathResource.exists()) {
                resolvedCredentialsResource = classpathResource;
            }
        } catch (Exception e) {
            log.debug("Classpath credentials not found: {}", e.getMessage());
        }
    }

    private List<Path> buildCredentialCandidates(String configuredPath) {
        String userDir = System.getProperty("user.dir");
        List<Path> candidates = new ArrayList<>();
        candidates.add(Path.of(configuredPath));
        candidates.add(Path.of(userDir, configuredPath));
        candidates.add(Path.of(userDir, "backend/microservices/recretment", configuredPath));
        candidates.add(Path.of(userDir, "microservices/recretment", configuredPath));
        candidates.add(Path.of(userDir, "recretment", configuredPath));
        return candidates;
    }

    private InputStream openCredentialsStream() throws Exception {
        if (resolvedCredentialsFile != null) {
            return new java.io.FileInputStream(resolvedCredentialsFile.toFile());
        }
        if (resolvedCredentialsResource != null) {
            return resolvedCredentialsResource.getInputStream();
        }
        throw new IllegalStateException("Google Calendar credentials not loaded");
    }

    private Calendar buildCalendarClient() throws Exception {
        GoogleCredentials credentials;
        try (InputStream stream = openCredentialsStream()) {
            if (StringUtils.hasText(delegateUser)) {
                credentials = ServiceAccountCredentials.fromStream(stream)
                        .createScoped(Collections.singleton(CalendarScopes.CALENDAR))
                        .createDelegated(delegateUser);
            } else {
                credentials = GoogleCredentials.fromStream(stream)
                        .createScoped(Collections.singleton(CalendarScopes.CALENDAR));
            }
        }

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private Event buildInterviewEvent(String title, LocalDateTime interviewAt, String candidateName) {
        ZoneId zoneId = ZoneId.of(timezone);
        LocalDateTime endAt = interviewAt.plusMinutes(durationMinutes);

        Event event = new Event()
                .setSummary(title)
                .setDescription("Entretien avec " + candidateName + " le " + interviewAt);

        EventDateTime start = new EventDateTime()
                .setDateTime(new DateTime(interviewAt.atZone(zoneId).toInstant().toEpochMilli()))
                .setTimeZone(timezone);
        EventDateTime end = new EventDateTime()
                .setDateTime(new DateTime(endAt.atZone(zoneId).toInstant().toEpochMilli()))
                .setTimeZone(timezone);
        event.setStart(start);
        event.setEnd(end);
        return event;
    }

    private String createViaCalendarApi(
            String title,
            LocalDateTime interviewAt,
            String candidateName) throws Exception {
        Calendar calendar = buildCalendarClient();
        Event event = buildInterviewEvent(title, interviewAt, candidateName);

        try {
            ConferenceSolutionKey solutionKey = new ConferenceSolutionKey().setType("hangoutsMeet");
            CreateConferenceRequest createRequest = new CreateConferenceRequest()
                    .setRequestId(UUID.randomUUID().toString())
                    .setConferenceSolutionKey(solutionKey);
            event.setConferenceData(new ConferenceData().setCreateRequest(createRequest));

            Event created = calendar.events()
                    .insert("primary", event)
                    .setConferenceDataVersion(1)
                    .execute();

            log.info("Calendar event with Meet created for {} at {}", title, interviewAt);

            if (StringUtils.hasText(created.getHangoutLink())) {
                return created.getHangoutLink();
            }
            if (created.getConferenceData() != null
                    && created.getConferenceData().getEntryPoints() != null
                    && !created.getConferenceData().getEntryPoints().isEmpty()) {
                return created.getConferenceData().getEntryPoints().get(0).getUri();
            }
            throw new IllegalStateException("Evenement Calendar cree mais aucun lien Google Meet retourne");
        } catch (GoogleJsonResponseException e) {
            if (!isConferenceCreationBlocked(e)) {
                throw e;
            }

            log.warn("Auto Google Meet not available for this account ({}). Creating calendar event + configured Meet link.",
                    e.getDetails() != null ? e.getDetails().getMessage() : e.getMessage());

            String meetLink = fallbackMeetLink();
            Event plainEvent = buildInterviewEvent(title, interviewAt, candidateName);
            plainEvent.setDescription(plainEvent.getDescription() + "\n\nLien Google Meet : " + meetLink);
            plainEvent.setLocation(meetLink);
            calendar.events().insert("primary", plainEvent).execute();
            log.info("Calendar event created for {} at {} with configured Meet link", title, interviewAt);
            return meetLink;
        }
    }

    private boolean isConferenceCreationBlocked(GoogleJsonResponseException e) {
        if (e.getStatusCode() != 400 && e.getStatusCode() != 403) {
            return false;
        }
        String message = e.getDetails() != null ? e.getDetails().getMessage() : e.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("conference")
                || lower.contains("forbiddenforserviceaccounts")
                || lower.contains("invalid conference type");
    }
}
