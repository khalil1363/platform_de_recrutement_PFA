package com.daam.recruitment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class CvStorageService {

    private static final Set<String> ALLOWED = Set.of(
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final Path uploadDir;

    public CvStorageService(@Value("${file.upload-dir:uploads/cv}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try { Files.createDirectories(this.uploadDir); } catch (IOException e) {
            throw new IllegalStateException("Cannot create CV upload directory");
        }
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("CV file is required");
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType))
            throw new IllegalArgumentException("Only PDF and Word documents are allowed");
        if (file.getSize() > 5 * 1024 * 1024) throw new IllegalArgumentException("CV must not exceed 5 MB");
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        try {
            Files.copy(file.getInputStream(), uploadDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            return "/api/recruitment/files/cv/" + filename;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to store CV");
        }
    }

    public Path getUploadDir() { return uploadDir; }
}
