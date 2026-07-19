package com.daam.recruitment.service;

import com.daam.recruitment.dto.RecruitmentDtos.UserSummary;
import com.daam.recruitment.entity.HiredQcmAssignment;
import com.daam.recruitment.entity.HiredQcmDimensionScore;
import com.daam.recruitment.psychometric.PsychometricDimensions;
import com.daam.recruitment.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TalentReportPdfService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy 'à' HH:mm", Locale.FRENCH);

    private final HiredQcmService hiredQcmService;

    @Transactional(readOnly = true)
    public byte[] generateForAssignment(String assignmentId, AuthUser authUser) {
        HiredQcmAssignment assignment = hiredQcmService.getCompletedAssignmentForRh(assignmentId, authUser);
        UserSummary candidate = hiredQcmService.loadCandidateSummary(assignment.getCandidateUserId());
        String qcmTitle = hiredQcmService.resolveQcmTitle(assignment.getQcmId());
        String recruitmentTitle = hiredQcmService.resolveRecruitmentTitle(assignment.getRecruitmentId());
        String companyName = hiredQcmService.resolveCompanyName(assignment.getRecruitmentId());
        List<HiredQcmDimensionScore> dimensions = hiredQcmService.getDimensionScores(assignmentId);

        String fullName = candidate != null
                ? ((candidate.getFirstName() != null ? candidate.getFirstName() : "") + " "
                + (candidate.getLastName() != null ? candidate.getLastName() : "")).trim()
                : "Candidat";

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Fonts fonts = loadFonts(doc);
            writeCover(doc, fonts, fullName, qcmTitle, recruitmentTitle, companyName, assignment);
            writeIntro(doc, fonts, fullName);
            writeInsight(doc, fonts, fullName, recruitmentTitle, assignment, dimensions);
            writeDetailTable(doc, fonts, fullName, dimensions);
            writeSummary(doc, fonts, fullName, dimensions, assignment);
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de générer le rapport PDF: " + e.getMessage(), e);
        }
    }

    private void writeCover(PDDocument doc, Fonts fonts, String fullName, String qcmTitle,
                            String recruitmentTitle, String companyName,
                            HiredQcmAssignment assignment) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            fillRect(cs, 0, 720, 595, 122, new Color(26, 92, 66));
            drawText(cs, fonts.bold, 28, 50, 780, "RAPPORT", Color.WHITE);
            drawText(cs, fonts.bold, 18, 50, 750, "Talent Insight — Profil PSY", Color.WHITE);
            drawText(cs, fonts.regular, 12, 50, 728, "DAAM RH · Évaluation post-embauche", new Color(220, 240, 230));

            drawText(cs, fonts.bold, 22, 50, 640, fullName, new Color(26, 92, 66));
            drawText(cs, fonts.regular, 12, 50, 615, qcmTitle, Color.DARK_GRAY);
            drawText(cs, fonts.regular, 11, 50, 595,
                    (companyName != null && !companyName.isBlank() ? companyName + " · " : "")
                            + (recruitmentTitle != null ? recruitmentTitle : ""),
                    Color.GRAY);

            if (assignment.getCompletedAt() != null) {
                drawText(cs, fonts.regular, 11, 50, 555,
                        "Test passé le " + assignment.getCompletedAt().format(DATE_FMT), Color.DARK_GRAY);
            }
            Integer fit = assignment.getOverallFitPercent();
            if (fit != null) {
                drawText(cs, fonts.bold, 36, 50, 500, fit + "%", new Color(26, 92, 66));
                drawText(cs, fonts.regular, 12, 50, 475, "Score global de correspondance au poste", Color.DARK_GRAY);
            }

            drawText(cs, fonts.regular, 9, 50, 80,
                    "Rapport confidentiel généré par DAAM. Ne pas diffuser sans consentement du candidat.",
                    Color.GRAY);
            drawText(cs, fonts.regular, 9, 50, 65,
                    "Structure inspirée des bilans Talent Insight / Profil Pro (usage interne DAAM).",
                    Color.GRAY);
        }
    }

    private void writeIntro(PDDocument doc, Fonts fonts, String fullName) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            sectionTitle(cs, fonts, "1 — INTRODUCTION");
            float y = 760;
            y = drawWrapped(cs, fonts.regular, 11, 50, y, 495,
                    "Ce rapport présente un résumé des résultats obtenus au questionnaire psychométrique "
                            + "assigné après confirmation d'embauche. Il analyse les compétences commerciales "
                            + "et comportements susceptibles d'influencer la performance sur un poste de type "
                            + "Commercial terrain / Chargé de crédit.", y);
            y -= 16;
            y = drawWrapped(cs, fonts.bold, 11, 50, y, 495, "Précautions d'interprétation :", y);
            y -= 8;
            String[] bullets = {
                    "Les scores doivent être lus ensemble et confrontés aux attentes du poste.",
                    "Ce rapport contient des commentaires d'aide à la décision RH ; un entretien de restitution est recommandé.",
                    "Complétez cette lecture par d'autres observations (entretien, période d'essai, performance).",
                    "Résultats confidentiels — ne pas communiquer à un tiers sans consentement de " + fullName + "."
            };
            for (String b : bullets) {
                y = drawWrapped(cs, fonts.regular, 10, 60, y, 480, "• " + b, y);
                y -= 6;
            }
        }
    }

    private void writeInsight(PDDocument doc, Fonts fonts, String fullName, String recruitmentTitle,
                              HiredQcmAssignment assignment,
                              List<HiredQcmDimensionScore> dimensions) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            sectionTitle(cs, fonts, "2 — TALENT INSIGHT");
            float y = 760;
            Integer fit = assignment.getOverallFitPercent() != null ? assignment.getOverallFitPercent() : 0;
            drawText(cs, fonts.bold, 28, 50, y, fit + "%", new Color(26, 92, 66));
            drawText(cs, fonts.regular, 11, 130, y + 8, "Modèle prédictif — " +
                    (recruitmentTitle != null && !recruitmentTitle.isBlank() ? recruitmentTitle : "Chargé de crédit"),
                    Color.DARK_GRAY);
            y -= 28;
            drawText(cs, fonts.regular, 10, 50, y,
                    "Positionnement de " + fullName + " par rapport aux compétences ciblées", Color.GRAY);
            y -= 24;

            List<HiredQcmDimensionScore> sorted = new ArrayList<>(dimensions);
            sorted.sort(Comparator.comparing(HiredQcmDimensionScore::getSortOrder,
                    Comparator.nullsLast(Integer::compareTo)));

            for (HiredQcmDimensionScore d : sorted) {
                if (y < 80) break;
                int pct = (int) Math.round((d.getScore() != null ? d.getScore() : 0) * 100);
                drawText(cs, fonts.regular, 10, 50, y, d.getDimensionLabel(), Color.DARK_GRAY);
                drawText(cs, fonts.bold, 10, 280, y, pct + "%", new Color(26, 92, 66));
                fillRect(cs, 320, y - 2, 200, 10, new Color(230, 235, 232));
                float fill = Math.max(2, 200f * pct / 100f);
                Color bar = pct >= (int) Math.round((d.getExpectedScore() != null ? d.getExpectedScore() : 0.7) * 100)
                        ? new Color(26, 92, 66) : new Color(200, 120, 40);
                fillRect(cs, 320, y - 2, fill, 10, bar);
                y -= 18;
            }
        }
    }

    private void writeDetailTable(PDDocument doc, Fonts fonts, String fullName,
                                  List<HiredQcmDimensionScore> dimensions) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        PDPageContentStream cs = new PDPageContentStream(doc, page);
        sectionTitle(cs, fonts, "3 — TABLEAU DES COMPÉTENCES");
        float y = 760;
            drawText(cs, fonts.regular, 9, 50, y, "Candidat : " + fullName, Color.GRAY);
            y -= 22;
            fillRect(cs, 45, y - 6, 505, 16, new Color(26, 92, 66));
            drawText(cs, fonts.bold, 9, 50, y, "Compétence", Color.WHITE);
            drawText(cs, fonts.bold, 9, 230, y, "Score", Color.WHITE);
            drawText(cs, fonts.bold, 9, 290, y, "Attendu", Color.WHITE);
            drawText(cs, fonts.bold, 9, 360, y, "Commentaire", Color.WHITE);
            y -= 20;

        for (HiredQcmDimensionScore d : dimensions) {
            if (y < 70) {
                cs.close();
                page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                cs = new PDPageContentStream(doc, page);
                y = 800;
            }
            int pct = (int) Math.round((d.getScore() != null ? d.getScore() : 0) * 100);
            int exp = (int) Math.round((d.getExpectedScore() != null ? d.getExpectedScore() : 0) * 100);
            drawText(cs, fonts.regular, 9, 50, y, truncate(d.getDimensionLabel(), 32), Color.DARK_GRAY);
            drawText(cs, fonts.bold, 9, 230, y, pct + "%", Color.BLACK);
            drawText(cs, fonts.regular, 9, 290, y, exp + "%", Color.GRAY);
            y = drawWrapped(cs, fonts.regular, 8, 360, y, 190,
                    d.getCommentText() != null ? d.getCommentText() : "", y);
            y -= 10;
        }
        cs.close();
    }

    private void writeSummary(PDDocument doc, Fonts fonts, String fullName,
                              List<HiredQcmDimensionScore> dimensions,
                              HiredQcmAssignment assignment) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            sectionTitle(cs, fonts, "4 — RÉSUMÉ DU PROFIL");
            float y = 760;

            List<HiredQcmDimensionScore> ranked = new ArrayList<>(dimensions);
            ranked.sort((a, b) -> Double.compare(
                    b.getScore() != null ? b.getScore() : 0,
                    a.getScore() != null ? a.getScore() : 0));

            drawText(cs, fonts.bold, 12, 50, y, "Points forts", new Color(26, 92, 66));
            y -= 18;
            for (HiredQcmDimensionScore d : ranked.stream().limit(4).toList()) {
                y = drawWrapped(cs, fonts.regular, 10, 50, y, 495,
                        "• " + d.getDimensionLabel() + " (" + Math.round((d.getScore() != null ? d.getScore() : 0) * 100)
                                + "%) — " + (d.getCommentText() != null ? d.getCommentText() : ""), y);
                y -= 8;
            }

            y -= 12;
            drawText(cs, fonts.bold, 12, 50, y, "Points de développement", new Color(180, 80, 40));
            y -= 18;
            List<HiredQcmDimensionScore> weak = new ArrayList<>(ranked);
            weak.sort(Comparator.comparing(d -> d.getScore() != null ? d.getScore() : 0));
            for (HiredQcmDimensionScore d : weak.stream().limit(3).toList()) {
                y = drawWrapped(cs, fonts.regular, 10, 50, y, 495,
                        "• " + d.getDimensionLabel() + " (" + Math.round((d.getScore() != null ? d.getScore() : 0) * 100)
                                + "%) — " + (d.getCommentText() != null ? d.getCommentText() : ""), y);
                y -= 8;
            }

            y -= 16;
            double fit = assignment.getOverallFitPercent() != null ? assignment.getOverallFitPercent() / 100.0 : 0;
            String global = PsychometricDimensions.globalComment(fit, 0.70);
            drawText(cs, fonts.bold, 12, 50, y, "Synthèse", new Color(26, 92, 66));
            y -= 18;
            y = drawWrapped(cs, fonts.regular, 11, 50, y, 495,
                    fullName + " : " + global, y);

            if (Boolean.TRUE.equals(assignment.getQcmViolated())) {
                y -= 20;
                drawText(cs, fonts.bold, 11, 50, y, "⚠ Test marqué pour comportement non conforme (score forcé à 0).",
                        new Color(180, 30, 30));
            }
        }
    }

    private void sectionTitle(PDPageContentStream cs, Fonts fonts, String title) throws IOException {
        fillRect(cs, 0, 800, 595, 42, new Color(26, 92, 66));
        drawText(cs, fonts.bold, 14, 50, 815, title, Color.WHITE);
    }

    private float drawWrapped(PDPageContentStream cs, Object font, float size, float x, float y,
                              float maxWidth, String text, float currentY) throws IOException {
        if (text == null) text = "";
        List<String> lines = wrap(font, size, text, maxWidth);
        float cursor = currentY;
        for (String line : lines) {
            drawText(cs, font, size, x, cursor, line, Color.DARK_GRAY);
            cursor -= size + 3;
        }
        return cursor;
    }

    private List<String> wrap(Object font, float size, String text, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String trial = line.isEmpty() ? word : line + " " + word;
            float w = stringWidth(font, size, trial);
            if (w > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(trial);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        if (lines.isEmpty()) lines.add("");
        return lines;
    }

    private float stringWidth(Object font, float size, String text) throws IOException {
        if (font instanceof PDType0Font f) {
            return f.getStringWidth(text) / 1000f * size;
        }
        if (font instanceof PDType1Font f) {
            return f.getStringWidth(text) / 1000f * size;
        }
        return text.length() * size * 0.5f;
    }

    private void drawText(PDPageContentStream cs, Object font, float size, float x, float y,
                          String text, Color color) throws IOException {
        if (text == null) text = "";
        // PDFBox Type1 fonts don't support many French accents; sanitize when fallback.
        String safe = text;
        cs.beginText();
        if (font instanceof PDType0Font f) {
            cs.setFont(f, size);
        } else {
            cs.setFont((PDType1Font) font, size);
            safe = stripAccents(text);
        }
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(safe.replace("\t", " ").replace("\r", " ").replace("\n", " "));
        cs.endText();
    }

    private void fillRect(PDPageContentStream cs, float x, float y, float w, float h, Color color)
            throws IOException {
        cs.setNonStrokingColor(color);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private String stripAccents(String input) {
        return java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    private Fonts loadFonts(PDDocument doc) throws IOException {
        File arial = new File("C:/Windows/Fonts/arial.ttf");
        File arialBold = new File("C:/Windows/Fonts/arialbd.ttf");
        if (arial.exists() && arialBold.exists()) {
            return new Fonts(PDType0Font.load(doc, arial), PDType0Font.load(doc, arialBold));
        }
        return new Fonts(
                new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD));
    }

    private record Fonts(Object regular, Object bold) {}
}
