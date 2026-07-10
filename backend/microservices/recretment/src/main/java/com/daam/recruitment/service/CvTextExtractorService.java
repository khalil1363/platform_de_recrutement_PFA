package com.daam.recruitment.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Service
public class CvTextExtractorService {

    public String extractText(Path file) {
        if (file == null || !Files.exists(file)) {
            throw new IllegalArgumentException("CV file not found");
        }
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".pdf")) {
                return extractPdf(file);
            }
            if (name.endsWith(".docx")) {
                return extractDocx(file);
            }
            if (name.endsWith(".doc")) {
                return extractDoc(file);
            }
            throw new IllegalArgumentException("Unsupported CV format: " + name);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read CV content: " + e.getMessage());
        }
    }

    private String extractPdf(Path file) throws Exception {
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return normalize(stripper.getText(document));
        }
    }

    private String extractDocx(Path file) throws Exception {
        try (InputStream in = Files.newInputStream(file);
             XWPFDocument document = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return normalize(extractor.getText());
        }
    }

    private String extractDoc(Path file) throws Exception {
        try (InputStream in = Files.newInputStream(file);
             HWPFDocument document = new HWPFDocument(in);
             WordExtractor extractor = new WordExtractor(document)) {
            return normalize(extractor.getText());
        }
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u0000', ' ').replaceAll("\\s+", " ").trim();
    }
}
