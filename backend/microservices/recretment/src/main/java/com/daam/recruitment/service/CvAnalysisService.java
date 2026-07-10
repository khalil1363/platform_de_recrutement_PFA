package com.daam.recruitment.service;

import com.daam.recruitment.entity.JobApplication;
import com.daam.recruitment.entity.Recruitment;
import com.daam.recruitment.repository.JobApplicationRepository;
import com.daam.recruitment.repository.RecruitmentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CvAnalysisService {

    private static final Pattern SKILL_SPLIT = Pattern.compile("[,;|/\\n•·\\-–—]+");
    private static final Pattern WORD_SPLIT = Pattern.compile("[^a-z0-9+#\\.]+");

    private final JobApplicationRepository jobApplicationRepository;
    private final RecruitmentRepository recruitmentRepository;
    private final CvStorageService cvStorageService;
    private final CvTextExtractorService cvTextExtractorService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.cv-analysis.openai-api-key:}")
    private String openaiApiKey;

    @Value("${app.cv-analysis.openai-model:gpt-4o-mini}")
    private String openaiModel;

    @Value("${app.cv-analysis.enabled:true}")
    private boolean enabled;

    @Async
    @Transactional
    public void analyzeApplicationAsync(String applicationId) {
        try {
            doAnalyze(applicationId);
        } catch (Exception e) {
            log.error("CV analysis failed for application {}: {}", applicationId, e.getMessage());
        }
    }

    @Transactional
    public JobApplication analyzeApplication(String applicationId) {
        return doAnalyze(applicationId);
    }

    private JobApplication doAnalyze(String applicationId) {
        if (!enabled) {
            return jobApplicationRepository.findByApplicationId(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        }

        JobApplication application = jobApplicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        Recruitment recruitment = recruitmentRepository.findByRecruitmentId(application.getRecruitmentId())
                .orElseThrow(() -> new IllegalArgumentException("Recruitment not found"));

        try {
            Path cvPath = resolveCvPath(application.getCvFileUrl());
            String cvText = cvTextExtractorService.extractText(cvPath);
            if (cvText.isBlank()) {
                return saveAnalysis(application, recruitment, 0, List.of(), List.of(),
                        extractJobSkills(recruitment),
                        "Impossible d'extraire le texte du CV. Vérifiez que le fichier PDF/Word n'est pas scanné.");
            }

            AnalysisResult result;
            if (openaiApiKey != null && !openaiApiKey.isBlank()) {
                try {
                    result = analyzeWithOpenAi(cvText, recruitment);
                } catch (Exception e) {
                    log.warn("OpenAI CV analysis failed, falling back to local matcher: {}", e.getMessage());
                    result = analyzeLocally(cvText, recruitment);
                }
            } else {
                result = analyzeLocally(cvText, recruitment);
            }

            return saveAnalysis(application, recruitment, result.matchScore(), result.extractedSkills(),
                    result.matchedSkills(), result.missingSkills(), result.summary());
        } catch (Exception e) {
            log.error("CV analysis failed for {}: {}", applicationId, e.getMessage());
            return saveAnalysis(application, recruitment, 0, List.of(), List.of(),
                    extractJobSkills(recruitment),
                    "Analyse CV échouée : " + e.getMessage());
        }
    }

    private JobApplication saveAnalysis(
            JobApplication application,
            Recruitment recruitment,
            int score,
            List<String> extracted,
            List<String> matched,
            List<String> missing,
            String summary
    ) {
        application.setCvMatchScore(score);
        application.setExtractedSkills(joinSkills(extracted));
        application.setMatchedSkills(joinSkills(matched));
        application.setMissingSkills(joinSkills(missing));
        application.setCvAnalysisSummary(summary);
        application.setCvAnalyzedAt(LocalDateTime.now());
        return jobApplicationRepository.save(application);
    }

    private AnalysisResult analyzeLocally(String cvText, Recruitment recruitment) {
        List<String> jobSkills = extractJobSkills(recruitment);
        String normalizedCv = normalize(cvText);

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String skill : jobSkills) {
            if (skillMatches(normalizedCv, skill)) {
                matched.add(skill);
            } else {
                missing.add(skill);
            }
        }

        List<String> extracted = new ArrayList<>(matched);
        for (String token : extractCandidateTokens(cvText)) {
            if (extracted.stream().noneMatch(s -> normalize(s).equals(normalize(token)))) {
                extracted.add(token);
            }
            if (extracted.size() >= 25) {
                break;
            }
        }

        int score;
        if (jobSkills.isEmpty()) {
            score = Math.min(70, 30 + extracted.size() * 2);
        } else {
            score = (int) Math.round((matched.size() * 100.0) / jobSkills.size());
        }

        String summary = buildLocalSummary(score, matched, missing, jobSkills.isEmpty());
        return new AnalysisResult(score, extracted, matched, missing, summary);
    }

    private AnalysisResult analyzeWithOpenAi(String cvText, Recruitment recruitment) throws Exception {
        String jobProfile = """
                Titre: %s
                Description: %s
                Responsabilités: %s
                Compétences techniques: %s
                Compétences personnelles: %s
                Formation: %s
                Expérience: %s
                Langues: %s
                """.formatted(
                nullToEmpty(recruitment.getTitle()),
                nullToEmpty(recruitment.getDescription()),
                nullToEmpty(recruitment.getResponsibilities()),
                nullToEmpty(recruitment.getTechnicalSkills()),
                nullToEmpty(recruitment.getPersonalSkills()),
                nullToEmpty(recruitment.getEducationRequirements()),
                nullToEmpty(recruitment.getExperienceRequirements()),
                recruitment.getLanguages() == null ? "" : String.join(", ", recruitment.getLanguages())
        );

        String truncatedCv = cvText.length() > 12000 ? cvText.substring(0, 12000) : cvText;
        String userPrompt = """
                Tu es un expert RH. Analyse le CV du candidat par rapport à l'offre d'emploi.
                Réponds UNIQUEMENT avec un JSON valide (sans markdown) de la forme:
                {
                  "matchScore": 0-100,
                  "extractedSkills": ["compétence1", "compétence2"],
                  "matchedSkills": ["compétence présente dans le CV et requise"],
                  "missingSkills": ["compétence requise absente du CV"],
                  "summary": "résumé court en français (2-3 phrases) pour le RH"
                }

                OFFRE:
                %s

                CV:
                %s
                """.formatted(jobProfile, truncatedCv);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", openaiModel);
        body.put("temperature", 0.2);
        body.put("messages", List.of(
                Map.of("role", "system", "content", "Tu analyses des CV pour le recrutement. Réponds uniquement en JSON."),
                Map.of("role", "user", "content", userPrompt)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);

        ResponseEntity<String> response = restTemplate.exchange(
                "https://api.openai.com/v1/chat/completions",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );

        JsonNode root = objectMapper.readTree(response.getBody());
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        content = content.replace("```json", "").replace("```", "").trim();
        JsonNode analysis = objectMapper.readTree(content);

        int score = Math.max(0, Math.min(100, analysis.path("matchScore").asInt(0)));
        List<String> extracted = readStringList(analysis.path("extractedSkills"));
        List<String> matched = readStringList(analysis.path("matchedSkills"));
        List<String> missing = readStringList(analysis.path("missingSkills"));
        String summary = analysis.path("summary").asText("Analyse CV terminée.");
        return new AnalysisResult(score, extracted, matched, missing, summary);
    }

    private List<String> extractJobSkills(Recruitment recruitment) {
        LinkedHashSet<String> skills = new LinkedHashSet<>();
        addSkillsFromText(skills, recruitment.getTechnicalSkills());
        addSkillsFromText(skills, recruitment.getPersonalSkills());
        addSkillsFromText(skills, recruitment.getExperienceRequirements());
        addSkillsFromText(skills, recruitment.getEducationRequirements());
        if (recruitment.getLanguages() != null) {
            recruitment.getLanguages().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> s.length() >= 2)
                    .forEach(skills::add);
        }
        return new ArrayList<>(skills);
    }

    private void addSkillsFromText(Set<String> skills, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        for (String part : SKILL_SPLIT.split(text)) {
            String skill = part.trim();
            if (skill.length() < 2 || skill.length() > 60) {
                continue;
            }
            if (skill.split("\\s+").length > 6) {
                continue;
            }
            skills.add(skill);
        }
    }

    private List<String> extractCandidateTokens(String cvText) {
        String[] known = {
                "java", "spring", "angular", "react", "python", "sql", "mysql", "postgresql",
                "docker", "kubernetes", "git", "jenkins", "aws", "azure", "linux", "javascript",
                "typescript", "nodejs", "php", "laravel", "symfony", "c#", ".net", "html", "css",
                "mongodb", "redis", "kafka", "microservices", "rest", "api", "scrum", "agile",
                "communication", "leadership", "teamwork", "anglais", "français", "arabe"
        };
        String normalized = normalize(cvText);
        List<String> found = new ArrayList<>();
        for (String skill : known) {
            if (normalized.contains(normalize(skill))) {
                found.add(skill);
            }
        }
        return found;
    }

    private boolean skillMatches(String normalizedCv, String skill) {
        String normalizedSkill = normalize(skill);
        if (normalizedSkill.length() < 2) {
            return false;
        }
        if (normalizedCv.contains(normalizedSkill)) {
            return true;
        }
        String[] parts = WORD_SPLIT.split(normalizedSkill);
        if (parts.length >= 2) {
            long hits = Arrays.stream(parts).filter(p -> p.length() >= 3 && normalizedCv.contains(p)).count();
            return hits >= Math.ceil(parts.length * 0.7);
        }
        return false;
    }

    private String buildLocalSummary(int score, List<String> matched, List<String> missing, boolean noJobSkills) {
        if (noJobSkills) {
            return "L'offre n'a pas de compétences structurées. Score estimé à " + score
                    + "% à partir du contenu du CV. Ajoutez des compétences techniques/personnelles sur l'offre pour un matching plus précis.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Compatibilité CV / poste : ").append(score).append("%. ");
        if (!matched.isEmpty()) {
            sb.append("Points forts : ").append(String.join(", ", matched.stream().limit(8).toList())).append(". ");
        }
        if (!missing.isEmpty()) {
            sb.append("Écarts : ").append(String.join(", ", missing.stream().limit(8).toList())).append(".");
        } else {
            sb.append("Le CV couvre bien les compétences demandées.");
        }
        return sb.toString().trim();
    }

    private Path resolveCvPath(String cvFileUrl) {
        if (cvFileUrl == null || cvFileUrl.isBlank()) {
            throw new IllegalArgumentException("No CV uploaded");
        }
        String filename = cvFileUrl;
        int slash = cvFileUrl.lastIndexOf('/');
        if (slash >= 0) {
            filename = cvFileUrl.substring(slash + 1);
        }

        List<Path> candidates = List.of(
                cvStorageService.getUploadDir().resolve(filename).normalize(),
                Paths.get("uploads", "cv", filename).toAbsolutePath().normalize(),
                Paths.get("..", "uploads", "cv", filename).toAbsolutePath().normalize(),
                Paths.get("..", "..", "uploads", "cv", filename).toAbsolutePath().normalize(),
                Paths.get("backend", "microservices", "recretment", "uploads", "cv", filename)
                        .toAbsolutePath().normalize()
        );

        for (Path path : candidates) {
            if (Files.exists(path) && Files.isRegularFile(path)) {
                return path;
            }
        }
        throw new IllegalArgumentException("CV file not found on disk: " + filename);
    }

    private List<String> readStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> {
                String v = n.asText("").trim();
                if (!v.isBlank()) {
                    values.add(v);
                }
            });
        }
        return values;
    }

    private String joinSkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return "";
        }
        return skills.stream().map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.joining(", "));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String n = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return n.replaceAll("\\s+", " ").trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record AnalysisResult(
            int matchScore,
            List<String> extractedSkills,
            List<String> matchedSkills,
            List<String> missingSkills,
            String summary
    ) {}
}
