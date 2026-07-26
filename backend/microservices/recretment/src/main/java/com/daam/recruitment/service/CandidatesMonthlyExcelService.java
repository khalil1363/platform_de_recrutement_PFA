package com.daam.recruitment.service;

import com.daam.recruitment.dto.RecruitmentDtos.ApplicationResponse;
import com.daam.recruitment.dto.RecruitmentDtos.UserSummary;
import com.daam.recruitment.enumeration.AgencyAffectation;
import com.daam.recruitment.enumeration.ApplicationStatus;
import com.daam.recruitment.enumeration.JobTitle;
import com.daam.recruitment.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Excel: one sheet per interview month + one "Recrutements Agences" index sheet.
 */
@Service
@RequiredArgsConstructor
public class CandidatesMonthlyExcelService {

    private static final String[] HEADERS = {
            "NOM DU CANDIDAT",
            "N° DE TELEPHONE",
            "EMAIL",
            "PROVENANCE",
            "IMF",
            "INTITULE DU POSTE",
            "AFFECTATION",
            "DATE DE L'ENTRETIEN",
            "HEURE DE L'ENTRETIEN",
            "STATUT",
            "HEBERGEMENT",
            "CONTRAT",
            "COMMENTAIRE",
            "DATE D'INTEGRATION",
            "PRETENTION",
            "DATE DE DEBUT POTENTIELLE",
            "RESPONSABLE DE RECRUTEMENT",
            "COWORKING"
    };

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Locale FR = Locale.FRENCH;

    private final RecruitmentService recruitmentService;

    @Transactional(readOnly = true)
    public byte[] exportForRh(AuthUser authUser) {
        List<ApplicationResponse> applications = filterMonthlyApplications(
                recruitmentService.getApplicationsForRh(authUser));

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ExcelStyles styles = createStyles(workbook);
            Set<String> usedSheetNames = new HashSet<>();
            writeMonthAndAgencySheets(workbook, applications, styles, usedSheetNames);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de générer l'export candidats: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportFullForRh(AuthUser authUser) {
        List<ApplicationResponse> allApplications = recruitmentService.getApplicationsForRh(authUser).stream()
                .sorted(Comparator.comparing(
                        ApplicationResponse::getAppliedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ExcelStyles styles = createStyles(workbook);
            Set<String> usedSheetNames = new HashSet<>();

            writeCrmSheet(workbook, allApplications, styles, usedSheetNames);

            Map<String, List<ApplicationResponse>> byRecruitmentAgency = allApplications.stream()
                    .collect(Collectors.groupingBy(
                            this::recruitmentAgencyKey,
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));
            writeRecruitmentAgencyIndex(
                    workbook,
                    byRecruitmentAgency,
                    styles.headerStyle(),
                    styles.titleStyle(),
                    styles.textStyle(),
                    styles.altRowStyle(),
                    usedSheetNames
            );
            writeListingSheet(workbook, styles, usedSheetNames);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de générer l'export complet: " + e.getMessage(), e);
        }
    }

    private List<ApplicationResponse> filterMonthlyApplications(List<ApplicationResponse> applications) {
        return applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.HIRED
                        || a.getStatus() == ApplicationStatus.REJECTED
                        || a.getStatus() == ApplicationStatus.DESISTED
                        || a.getStatus() == ApplicationStatus.ACCEPTED)
                .toList();
    }

    private void writeMonthAndAgencySheets(
            XSSFWorkbook workbook,
            List<ApplicationResponse> applications,
            ExcelStyles styles,
            Set<String> usedSheetNames) {
        Map<YearMonth, List<ApplicationResponse>> byMonth = applications.stream()
                .collect(Collectors.groupingBy(
                        this::monthKey,
                        TreeMap::new,
                        Collectors.toList()
                ));

        Map<String, List<ApplicationResponse>> byRecruitmentAgency = applications.stream()
                .collect(Collectors.groupingBy(
                        this::recruitmentAgencyKey,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        if (byMonth.isEmpty()) {
            Sheet empty = workbook.createSheet(uniqueSheetName("Vide", usedSheetNames));
            empty.createRow(0).createCell(0).setCellValue(
                    "Aucune candidature (Retenu / Non retenu / Désisté) pour l'export mensuel.");
        } else {
            for (Map.Entry<YearMonth, List<ApplicationResponse>> entry : byMonth.entrySet()) {
                YearMonth ym = entry.getKey();
                List<ApplicationResponse> monthRows = entry.getValue().stream()
                        .sorted(Comparator
                                .comparing(this::sortDate, Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(a -> a.getStatus() == ApplicationStatus.HIRED
                                        || a.getStatus() == ApplicationStatus.ACCEPTED ? 0 : 1))
                        .toList();

                String monthLabel = sheetNameFor(ym);
                writeCandidatesSheet(
                        workbook,
                        uniqueSheetName(monthLabel, usedSheetNames),
                        "DAAM - CANDIDATS " + monthLabel.toUpperCase(FR),
                        monthRows,
                        styles.headerStyle(),
                        styles.titleStyle(),
                        styles.textStyle(),
                        styles.altRowStyle()
                );
            }
        }

        writeRecruitmentAgencyIndex(
                workbook,
                byRecruitmentAgency,
                styles.headerStyle(),
                styles.titleStyle(),
                styles.textStyle(),
                styles.altRowStyle(),
                usedSheetNames
        );
        writeListingSheet(workbook, styles, usedSheetNames);
    }

    private void writeListingSheet(XSSFWorkbook workbook, ExcelStyles styles, Set<String> usedSheetNames) {
        Sheet sheet = workbook.createSheet(uniqueSheetName("listing", usedSheetNames));

        Row title = sheet.createRow(0);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue("DAAM - RÉFÉRENTIELS — AGENCES & POSTES");
        titleCell.setCellStyle(styles.titleStyle());
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        Row header = sheet.createRow(2);
        header.setHeightInPoints(28);
        Cell affectationHeader = header.createCell(0);
        affectationHeader.setCellValue("AFFECTATION");
        affectationHeader.setCellStyle(styles.headerStyle());
        Cell posteHeader = header.createCell(1);
        posteHeader.setCellValue("POSTE");
        posteHeader.setCellStyle(styles.headerStyle());

        List<String> affectations = AgencyAffectation.labels();
        List<String> postes = JobTitle.labels();
        int rowCount = Math.max(affectations.size(), postes.size());

        for (int i = 0; i < rowCount; i++) {
            Row row = sheet.createRow(i + 3);
            CellStyle style = ((i + 3) % 2 == 0) ? styles.altRowStyle() : styles.textStyle();

            Cell affectationCell = row.createCell(0);
            affectationCell.setCellValue(i < affectations.size() ? affectations.get(i) : "");
            affectationCell.setCellStyle(style);

            Cell posteCell = row.createCell(1);
            posteCell.setCellValue(i < postes.size() ? postes.get(i) : "");
            posteCell.setCellStyle(style);
        }

        sheet.setColumnWidth(0, 28 * 256);
        sheet.setColumnWidth(1, 48 * 256);
        sheet.createFreezePane(0, 3);
        if (rowCount > 0) {
            sheet.setAutoFilter(new CellRangeAddress(2, 2 + rowCount, 0, 1));
        }
    }

    private void writeCrmSheet(
            XSSFWorkbook workbook,
            List<ApplicationResponse> applications,
            ExcelStyles styles,
            Set<String> usedSheetNames) {
        Sheet sheet = workbook.createSheet(uniqueSheetName("Suivi CRM", usedSheetNames));

        Row title = sheet.createRow(0);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue("DAAM - SUIVI CANDIDATS (CRM)");
        titleCell.setCellStyle(styles.titleStyle());
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, CRM_HEADERS.length - 1));

        Row header = sheet.createRow(2);
        header.setHeightInPoints(32);
        for (int i = 0; i < CRM_HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(CRM_HEADERS[i]);
            cell.setCellStyle(styles.headerStyle());
        }

        int r = 3;
        int id = 1;
        for (ApplicationResponse app : applications) {
            Row row = sheet.createRow(r);
            CellStyle style = (r % 2 == 0) ? styles.altRowStyle() : styles.textStyle();
            String[] values = toCrmRow(app, id++);
            for (int c = 0; c < values.length; c++) {
                Cell cell = row.createCell(c);
                cell.setCellValue(values[c] != null ? values[c] : "");
                cell.setCellStyle(style);
            }
            r++;
        }

        for (int i = 0; i < CRM_HEADERS.length; i++) {
            sheet.setColumnWidth(i, crmColumnWidth(i));
        }
        sheet.createFreezePane(0, 3);
        if (r > 3) {
            sheet.setAutoFilter(new CellRangeAddress(2, r - 1, 0, CRM_HEADERS.length - 1));
        }
    }

    private static final String[] CRM_HEADERS = {
            "ID",
            "Nom Complet",
            "Téléphone",
            "Email",
            "Source",
            "Référence",
            "Ancien Employeur",
            "Exp (ans)",
            "Poste Ciblé",
            "Affectation / Agence",
            "Type Contrat",
            "Prétention (DT)",
            "Date Formation",
            "Date Début",
            "Email Confirmé",
            "Hébergement",
            "Date Entretien RH",
            "Heure RH",
            "Statut RH",
            "Commentaire RH",
            "Date Entretien Resp.",
            "Heure Resp.",
            "Statut Responsable",
            "Commentaire Resp.",
            "Délai (jours)",
            "Sélectionné"
    };

    private String[] toCrmRow(ApplicationResponse app, int id) {
        UserSummary c = app.getCandidate();
        String fullName = "";
        if (c != null) {
            fullName = ((c.getFirstName() != null ? c.getFirstName() : "") + " "
                    + (c.getLastName() != null ? c.getLastName() : "")).trim();
        }

        String source = firstNonBlank(
                app.getProvenance(),
                app.getKeejobReference() != null && !app.getKeejobReference().isBlank() ? "KEEJOB" : null,
                "Plateforme DAAM"
        );
        String reference = firstNonBlank(app.getKeejobReference(), app.getInternalReference(), app.getCodeDossier());
        String ancienEmployeur = firstNonBlank(app.getSituationPerso(), app.getCommercialName(), app.getImf());
        String poste = firstNonBlank(app.getProfilMetier(), app.getRecruitmentTitle());
        String affectation = firstNonBlank(app.getAffectation(), resolveAgenceName(app));
        String contrat = firstNonBlank(app.getDureeContrat(), app.getHireContractType(), app.getFormatMission());
        String pretention = firstNonBlank(app.getPretention(), app.getHireNetSalary(), app.getSalaireActuel(), app.getPrixMois());
        String dateFormation = "";
        String dateDebut = firstNonBlank(
                app.getHireStartDate() != null ? app.getHireStartDate().format(DATE_FMT) : null,
                app.getDateDebutMission() != null ? app.getDateDebutMission().format(DATE_FMT) : null
        );
        boolean emailConfirmed = app.getStatus() == ApplicationStatus.ACCEPTED
                || app.getStatus() == ApplicationStatus.HIRED;
        String hebergement = firstNonBlank(app.getHebergement());
        String interviewDate = app.getInterviewAt() != null ? app.getInterviewAt().format(DATE_FMT) : "";
        String interviewTime = app.getInterviewAt() != null ? app.getInterviewAt().format(TIME_FMT) : "";
        String commentaireRh = firstNonBlank(app.getCommentairesRh());
        String commentaireResp = firstNonBlank(app.getRemarquesRh(), app.getObservation());
        String delaiJours = "";
        if (app.getAppliedAt() != null && app.getInterviewAt() != null) {
            long days = java.time.Duration.between(
                    app.getAppliedAt().toLocalDate().atStartOfDay(),
                    app.getInterviewAt().toLocalDate().atStartOfDay()
            ).toDays();
            delaiJours = String.valueOf(days);
        } else if (app.getAppliedAt() != null && app.getHiredAt() != null) {
            long days = java.time.Duration.between(
                    app.getAppliedAt().toLocalDate().atStartOfDay(),
                    app.getHiredAt().toLocalDate().atStartOfDay()
            ).toDays();
            delaiJours = String.valueOf(days);
        }
        String selectionne = (app.getStatus() == ApplicationStatus.ACCEPTED
                || app.getStatus() == ApplicationStatus.HIRED) ? "Oui" : "Non";

        return new String[]{
                String.valueOf(id),
                fullName,
                c != null && c.getPhoneNumber() != null ? c.getPhoneNumber() : "",
                c != null && c.getEmail() != null ? c.getEmail() : "",
                source != null ? source : "",
                reference != null ? reference : "",
                ancienEmployeur != null ? ancienEmployeur : "",
                app.getExperienceYears() != null ? app.getExperienceYears() : "",
                poste != null ? poste : "",
                affectation != null ? affectation : "",
                contrat != null ? contrat : "",
                pretention != null ? pretention : "",
                dateFormation,
                dateDebut != null ? dateDebut : "",
                emailConfirmed ? "Oui" : "Non",
                hebergement != null ? hebergement : "",
                interviewDate,
                interviewTime,
                rhStatusLabel(app.getStatus()),
                commentaireRh != null ? commentaireRh : "",
                "",
                "",
                responsableStatusLabel(app.getStatus()),
                commentaireResp != null ? commentaireResp : "",
                delaiJours,
                selectionne
        };
    }

    private String rhStatusLabel(ApplicationStatus status) {
        if (status == null) return "";
        return switch (status) {
            case SUBMITTED -> "EN ATTENTE";
            case UNDER_REVIEW -> "EN ATTENTE";
            case ACCEPTED, HIRED -> "RETENU";
            case REJECTED -> "NON RETENU";
            case DESISTED -> "DÉSISTÉ";
        };
    }

    private String responsableStatusLabel(ApplicationStatus status) {
        if (status == null) return "";
        return switch (status) {
            case HIRED, ACCEPTED -> "Retenu";
            case REJECTED -> "Non retenu";
            case DESISTED -> "Désisté";
            default -> "";
        };
    }

    private ExcelStyles createStyles(XSSFWorkbook workbook) {
        XSSFCellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0, (byte) 51, (byte) 102}, null));
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setWrapText(true);
        setThinBorders(headerStyle);

        XSSFCellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 12);
        titleStyle.setFont(titleFont);

        CellStyle textStyle = workbook.createCellStyle();
        setThinBorders(textStyle);
        textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        textStyle.setWrapText(true);

        XSSFCellStyle altRowStyle = workbook.createCellStyle();
        altRowStyle.cloneStyleFrom(textStyle);
        altRowStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 226, (byte) 239, (byte) 218}, null));
        altRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return new ExcelStyles(headerStyle, titleStyle, textStyle, altRowStyle);
    }

    private record ExcelStyles(
            XSSFCellStyle headerStyle,
            XSSFCellStyle titleStyle,
            CellStyle textStyle,
            XSSFCellStyle altRowStyle) {
    }

    private void writeCandidatesSheet(
            XSSFWorkbook workbook,
            String sheetName,
            String titleText,
            List<ApplicationResponse> rows,
            XSSFCellStyle headerStyle,
            XSSFCellStyle titleStyle,
            CellStyle textStyle,
            XSSFCellStyle altRowStyle) {
        Sheet sheet = workbook.createSheet(sheetName);

        Row title = sheet.createRow(0);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue(titleText);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));

        Row header = sheet.createRow(2);
        header.setHeightInPoints(32);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        int r = 3;
        for (ApplicationResponse app : rows) {
            Row row = sheet.createRow(r);
            CellStyle style = (r % 2 == 0) ? altRowStyle : textStyle;
            String[] values = toRow(app);
            for (int c = 0; c < values.length; c++) {
                Cell cell = row.createCell(c);
                cell.setCellValue(values[c] != null ? values[c] : "");
                cell.setCellStyle(style);
            }
            r++;
        }

        for (int i = 0; i < HEADERS.length; i++) {
            sheet.setColumnWidth(i, columnWidth(i));
        }
        sheet.createFreezePane(0, 3);
        sheet.setAutoFilter(new CellRangeAddress(2, Math.max(2, r - 1), 0, HEADERS.length - 1));
    }

    private void writeRecruitmentAgencyIndex(
            XSSFWorkbook workbook,
            Map<String, List<ApplicationResponse>> byRecruitmentAgency,
            XSSFCellStyle headerStyle,
            XSSFCellStyle titleStyle,
            CellStyle textStyle,
            XSSFCellStyle altRowStyle,
            Set<String> usedSheetNames) {
        Sheet sheet = workbook.createSheet(uniqueSheetName("Recrutements Agences", usedSheetNames));
        Row title = sheet.createRow(0);
        title.createCell(0).setCellValue("DAAM - RECRUTEMENTS ET AGENCES");
        title.getCell(0).setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        String[] indexHeaders = {"RECRUTEMENT", "AGENCE", "NB CANDIDATS", "MOIS"};
        Row header = sheet.createRow(2);
        for (int i = 0; i < indexHeaders.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(indexHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        int r = 3;
        for (List<ApplicationResponse> group : byRecruitmentAgency.values()) {
            if (group.isEmpty()) continue;
            ApplicationResponse sample = group.get(0);
            String months = group.stream()
                    .map(this::monthKey)
                    .distinct()
                    .sorted()
                    .map(this::sheetNameFor)
                    .collect(Collectors.joining(", "));
            Row row = sheet.createRow(r);
            CellStyle style = (r % 2 == 0) ? altRowStyle : textStyle;
            String[] values = {
                    firstNonBlank(sample.getRecruitmentTitle(), "-"),
                    resolveAgenceName(sample),
                    String.valueOf(group.size()),
                    months
            };
            for (int c = 0; c < values.length; c++) {
                Cell cell = row.createCell(c);
                cell.setCellValue(values[c]);
                cell.setCellStyle(style);
            }
            r++;
        }
        sheet.setColumnWidth(0, 36 * 256);
        sheet.setColumnWidth(1, 28 * 256);
        sheet.setColumnWidth(2, 14 * 256);
        sheet.setColumnWidth(3, 28 * 256);
        sheet.createFreezePane(0, 3);
        if (r > 3) {
            sheet.setAutoFilter(new CellRangeAddress(2, r - 1, 0, 3));
        }
    }

    private String[] toRow(ApplicationResponse app) {
        UserSummary c = app.getCandidate();
        String fullName = "";
        if (c != null) {
            fullName = ((c.getFirstName() != null ? c.getFirstName() : "") + " "
                    + (c.getLastName() != null ? c.getLastName() : "")).trim().toUpperCase(FR);
        }

        String provenance = firstNonBlank(
                app.getProvenance(),
                app.getKeejobReference() != null && !app.getKeejobReference().isBlank() ? "KEEJOB" : null,
                "Plateforme DAAM"
        );
        String imf = firstNonBlank(app.getImf(), app.getDiplomeEcole());
        String poste = firstNonBlank(app.getRecruitmentTitle(), app.getProfilMetier());
        String affectation = firstNonBlank(app.getAffectation(), resolveAgenceName(app));
        String interviewDate = app.getInterviewAt() != null ? app.getInterviewAt().format(DATE_FMT) : "";
        String interviewTime = app.getInterviewAt() != null ? app.getInterviewAt().format(TIME_FMT) : "";
        String hebergement = firstNonBlank(app.getHebergement());
        String contrat = firstNonBlank(app.getDureeContrat(), app.getHireContractType(), app.getFormatMission());
        String commentaire = firstNonBlank(app.getCommentairesRh(), app.getRemarquesRh(), app.getObservation());
        String dateIntegration = firstNonBlank(
                app.getHireStartDate() != null ? app.getHireStartDate().format(DATE_FMT) : null,
                app.getDateDebutMission() != null ? app.getDateDebutMission().format(DATE_FMT) : null
        );
        String pretention = firstNonBlank(app.getPretention(), app.getHireNetSalary(), app.getSalaireActuel(), app.getPrixMois());
        String dateDebutPotentielle = app.getDateDebutPotentielle() != null
                ? app.getDateDebutPotentielle().format(DATE_FMT)
                : "";
        String responsable = firstNonBlank(app.getResponsibleName(), app.getContactName());
        String coworking = "Non";
        if (app.isCoworking()) {
            if (app.getCoworkingMonth() != null) {
                YearMonth ym = YearMonth.from(app.getCoworkingMonth());
                String month = ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, FR);
                month = month.substring(0, 1).toUpperCase(FR) + month.substring(1);
                coworking = "Oui (" + month + " " + ym.getYear() + ")";
            } else {
                coworking = "Oui";
            }
        }

        return new String[]{
                fullName,
                c != null && c.getPhoneNumber() != null ? c.getPhoneNumber() : "",
                c != null && c.getEmail() != null ? c.getEmail() : "",
                provenance != null ? provenance : "",
                imf != null ? imf : "",
                poste != null ? poste.toUpperCase(FR) : "",
                affectation != null ? affectation : "",
                interviewDate,
                interviewTime,
                statusLabel(app.getStatus()),
                hebergement != null ? hebergement : "",
                contrat != null ? contrat : "",
                commentaire != null ? commentaire : "",
                dateIntegration != null ? dateIntegration : "",
                pretention != null ? pretention : "",
                dateDebutPotentielle,
                responsable != null ? responsable : "",
                coworking
        };
    }

    private String resolveAgenceName(ApplicationResponse app) {
        return firstNonBlank(app.getCompanyName(), app.getZoneName(), "Agence non renseignée");
    }

    private String recruitmentAgencyKey(ApplicationResponse app) {
        String recruitmentId = firstNonBlank(app.getRecruitmentId(), app.getRecruitmentTitle(), "unknown");
        String agence = resolveAgenceName(app);
        return recruitmentId + "||" + agence;
    }

    private String statusLabel(ApplicationStatus status) {
        if (status == null) return "";
        return switch (status) {
            case SUBMITTED, UNDER_REVIEW -> "En attente";
            case ACCEPTED, HIRED -> "Retenu";
            case REJECTED -> "Non retenu";
            case DESISTED -> "Désisté";
        };
    }

    /** Month sheet key: interview date, else hire date, else application date. */
    private YearMonth monthKey(ApplicationResponse a) {
        if (a.getInterviewAt() != null) {
            return YearMonth.from(a.getInterviewAt());
        }
        if (a.getHiredAt() != null) {
            return YearMonth.from(a.getHiredAt());
        }
        if (a.getAppliedAt() != null) {
            return YearMonth.from(a.getAppliedAt());
        }
        return YearMonth.now();
    }

    private java.time.LocalDateTime sortDate(ApplicationResponse a) {
        if (a.getInterviewAt() != null) return a.getInterviewAt();
        if (a.getHiredAt() != null) return a.getHiredAt();
        return a.getAppliedAt();
    }

    private String sheetNameFor(YearMonth ym) {
        String month = ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, FR);
        month = month.substring(0, 1).toUpperCase(FR) + month.substring(1);
        return sanitizeSheetName(month + " " + ym.getYear());
    }

    private String sanitizeSheetName(String name) {
        String cleaned = name.replaceAll("[\\\\/?*\\[\\]:]", " ").replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) {
            cleaned = "Feuille";
        }
        return cleaned.substring(0, Math.min(31, cleaned.length()));
    }

    private String uniqueSheetName(String base, Set<String> used) {
        String candidate = sanitizeSheetName(base);
        if (!used.contains(candidate.toLowerCase(FR))) {
            used.add(candidate.toLowerCase(FR));
            return candidate;
        }
        int i = 2;
        while (true) {
            String suffix = " " + i;
            int max = Math.max(1, 31 - suffix.length());
            String next = sanitizeSheetName(candidate.substring(0, Math.min(max, candidate.length())) + suffix);
            if (!used.contains(next.toLowerCase(FR))) {
                used.add(next.toLowerCase(FR));
                return next;
            }
            i++;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private int crmColumnWidth(int index) {
        return switch (index) {
            case 0 -> 6 * 256;
            case 1 -> 24 * 256;
            case 3 -> 26 * 256;
            case 8 -> 26 * 256;
            case 9 -> 24 * 256;
            case 19, 23 -> 28 * 256;
            case 25 -> 12 * 256;
            default -> 14 * 256;
        };
    }

    private int columnWidth(int index) {
        return switch (index) {
            case 0 -> 28 * 256;
            case 1 -> 16 * 256;
            case 2 -> 26 * 256;
            case 5 -> 28 * 256;
            case 6 -> 24 * 256;
            case 10 -> 18 * 256;
            case 12 -> 30 * 256;
            case 16 -> 26 * 256;
            case 17 -> 18 * 256;
            default -> 16 * 256;
        };
    }

    private void setThinBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
