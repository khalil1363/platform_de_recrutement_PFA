package com.daam.recruitment.service;

import com.daam.recruitment.dto.RecruitmentDtos.UserSummary;
import com.daam.recruitment.entity.HiredQcmAssignment;
import com.daam.recruitment.entity.HiredQcmDimensionScore;
import com.daam.recruitment.psychometric.PsychometricDimensions;
import com.daam.recruitment.psychometric.PsychometricDimensions.DimensionDef;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel "Suivi Test PSY CC" — one multi-row block per candidate
 * (Score global + 18 compétences), same layout as the reference screenshot.
 */
@Service
@RequiredArgsConstructor
public class HiredEvaluationsExcelService {

    private static final double GLOBAL_EXPECTED = 0.70;

    private final HiredQcmService hiredQcmService;

    @Transactional(readOnly = true)
    public byte[] exportCompletedEvaluations(AuthUser authUser) {
        List<HiredQcmAssignment> assignments = hiredQcmService.listCompletedForRhZone(authUser);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("KPIs Commercial Terrain");

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            XSSFCellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 189, (byte) 215, (byte) 238}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setWrapText(true);

            XSSFCellStyle expectedHeaderStyle = workbook.createCellStyle();
            expectedHeaderStyle.cloneStyleFrom(headerStyle);
            expectedHeaderStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0, (byte) 112, (byte) 192}, null));
            Font whiteFont = workbook.createFont();
            whiteFont.setBold(true);
            whiteFont.setColor(IndexedColors.WHITE.getIndex());
            expectedHeaderStyle.setFont(whiteFont);

            CellStyle pctStyle = workbook.createCellStyle();
            pctStyle.setDataFormat(workbook.createDataFormat().getFormat("0%"));
            pctStyle.setBorderBottom(BorderStyle.THIN);
            pctStyle.setBorderTop(BorderStyle.THIN);
            pctStyle.setBorderLeft(BorderStyle.THIN);
            pctStyle.setBorderRight(BorderStyle.THIN);
            pctStyle.setAlignment(HorizontalAlignment.CENTER);

            XSSFCellStyle expectedPctStyle = workbook.createCellStyle();
            expectedPctStyle.cloneStyleFrom(pctStyle);
            expectedPctStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 217, (byte) 226, (byte) 243}, null));
            expectedPctStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setBorderBottom(BorderStyle.THIN);
            textStyle.setBorderTop(BorderStyle.THIN);
            textStyle.setBorderLeft(BorderStyle.THIN);
            textStyle.setBorderRight(BorderStyle.THIN);
            textStyle.setWrapText(true);
            textStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle nameStyle = workbook.createCellStyle();
            nameStyle.cloneStyleFrom(textStyle);
            Font nameFont = workbook.createFont();
            nameFont.setBold(true);
            nameStyle.setFont(nameFont);
            nameStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Row title = sheet.createRow(1);
            title.createCell(0).setCellValue(
                    "Talent Review_Profil Commercial / Chargé(e) de Crédit");
            title.getCell(0).setCellStyle(titleStyle);

            Row header = sheet.createRow(4);
            header.createCell(0).setCellValue("Nom & Prénom / Candidat");
            header.createCell(1).setCellValue("Compétences");
            header.createCell(2).setCellValue("Score / %");
            header.createCell(3).setCellValue("Score attendu poste (%)");
            header.createCell(4).setCellValue("Interprétation / Commentaire");
            header.getCell(0).setCellStyle(headerStyle);
            header.getCell(1).setCellStyle(headerStyle);
            header.getCell(2).setCellStyle(headerStyle);
            header.getCell(3).setCellStyle(expectedHeaderStyle);
            header.getCell(4).setCellStyle(headerStyle);
            header.setHeightInPoints(30);

            int rowIdx = 5;
            List<DimensionDef> catalog = PsychometricDimensions.all();

            for (HiredQcmAssignment assignment : assignments) {
                UserSummary candidate = hiredQcmService.loadCandidateSummary(assignment.getCandidateUserId());
                String fullName = candidate != null
                        ? ((candidate.getFirstName() != null ? candidate.getFirstName() : "") + " "
                        + (candidate.getLastName() != null ? candidate.getLastName() : "")).trim()
                        : assignment.getCandidateUserId();
                if (fullName == null || fullName.isBlank()) {
                    fullName = "Candidat";
                }

                Map<String, HiredQcmDimensionScore> byCode = new LinkedHashMap<>();
                for (HiredQcmDimensionScore d : hiredQcmService.getDimensionScores(assignment.getAssignmentId())) {
                    if (d.getDimensionCode() != null) {
                        byCode.put(d.getDimensionCode().toUpperCase(), d);
                    }
                }

                double overall = assignment.getOverallFitPercent() != null
                        ? assignment.getOverallFitPercent() / 100.0
                        : averageScores(byCode);

                int blockStart = rowIdx;

                // --- Score global ---
                Row global = sheet.createRow(rowIdx++);
                Cell nameCell = global.createCell(0);
                nameCell.setCellValue(fullName);
                nameCell.setCellStyle(nameStyle);

                Cell gLabel = global.createCell(1);
                gLabel.setCellValue("Score global");
                gLabel.setCellStyle(textStyle);

                Cell gScore = global.createCell(2);
                gScore.setCellValue(overall);
                gScore.setCellStyle(pctStyle);

                Cell gExp = global.createCell(3);
                gExp.setCellValue(GLOBAL_EXPECTED);
                gExp.setCellStyle(expectedPctStyle);

                Cell gComment = global.createCell(4);
                gComment.setCellValue(PsychometricDimensions.globalComment(overall, GLOBAL_EXPECTED));
                gComment.setCellStyle(textStyle);

                // --- 18 compétences (always, same order as screenshot) ---
                for (DimensionDef def : catalog) {
                    HiredQcmDimensionScore scored = byCode.get(def.code());
                    double score = scored != null && scored.getScore() != null ? scored.getScore() : 0.0;
                    double expected = scored != null && scored.getExpectedScore() != null
                            ? scored.getExpectedScore()
                            : def.expectedScore();
                    String comment = scored != null && scored.getCommentText() != null && !scored.getCommentText().isBlank()
                            ? scored.getCommentText()
                            : PsychometricDimensions.commentFor(def, score);

                    Row row = sheet.createRow(rowIdx++);
                    // empty name cell (merged later)
                    Cell emptyName = row.createCell(0);
                    emptyName.setCellStyle(nameStyle);

                    Cell label = row.createCell(1);
                    label.setCellValue(def.label());
                    label.setCellStyle(textStyle);

                    Cell scoreCell = row.createCell(2);
                    scoreCell.setCellValue(score);
                    scoreCell.setCellStyle(pctStyle);

                    Cell expCell = row.createCell(3);
                    expCell.setCellValue(expected);
                    expCell.setCellStyle(expectedPctStyle);

                    Cell commentCell = row.createCell(4);
                    commentCell.setCellValue(comment);
                    commentCell.setCellStyle(textStyle);
                }

                int blockEnd = rowIdx - 1;
                if (blockEnd > blockStart) {
                    sheet.addMergedRegion(new CellRangeAddress(blockStart, blockEnd, 0, 0));
                }

                // spacer row between candidates
                rowIdx++;
            }

            sheet.setColumnWidth(0, 28 * 256);
            sheet.setColumnWidth(1, 28 * 256);
            sheet.setColumnWidth(2, 14 * 256);
            sheet.setColumnWidth(3, 22 * 256);
            sheet.setColumnWidth(4, 70 * 256);

            // mark unused param if empty export
            if (assignments.isEmpty()) {
                Row empty = sheet.createRow(rowIdx);
                empty.createCell(0).setCellValue("Aucune évaluation terminée pour votre zone.");
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de générer l'export Excel: " + e.getMessage(), e);
        }
    }

    private double averageScores(Map<String, HiredQcmDimensionScore> byCode) {
        if (byCode.isEmpty()) {
            return 0;
        }
        return byCode.values().stream()
                .mapToDouble(d -> d.getScore() != null ? d.getScore() : 0)
                .average()
                .orElse(0);
    }
}
