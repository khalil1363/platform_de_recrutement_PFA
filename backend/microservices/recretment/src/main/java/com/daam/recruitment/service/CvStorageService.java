package com.daam.recruitment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class CvStorageService {

    private static final Set<String> ALLOWED = Set.of(
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final Path uploadDir;

    public CvStorageService(@Value("${file.upload-dir:uploads/cv}") String uploadDir) {
        this.uploadDir = resolveUploadDir(uploadDir);
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create CV upload directory: " + this.uploadDir);
        }
    }

    private Path resolveUploadDir(String configured) {
        List<Path> candidates = List.of(
                Paths.get(configured).toAbsolutePath().normalize(),
                Paths.get("uploads", "cv").toAbsolutePath().normalize(),
                Paths.get("..", "uploads", "cv").toAbsolutePath().normalize(),
                Paths.get("..", "..", "uploads", "cv").toAbsolutePath().normalize(),
                Paths.get("backend", "microservices", "recretment", "uploads", "cv").toAbsolutePath().normalize()
        );

        Path bestExisting = null;
        long bestCount = -1;
        for (Path candidate : candidates) {
            if (!Files.isDirectory(candidate)) {
                continue;
            }
            long count = countFiles(candidate);
            if (count > bestCount) {
                bestCount = count;
                bestExisting = candidate;
            }
        }
        if (bestExisting != null && bestCount > 0) {
            return bestExisting;
        }
        if (bestExisting != null) {
            return bestExisting;
        }
        return Paths.get(configured).toAbsolutePath().normalize();
    }

    private long countFiles(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            return 0;
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

    public Path getUploadDir() {
        return uploadDir;
    }
}
