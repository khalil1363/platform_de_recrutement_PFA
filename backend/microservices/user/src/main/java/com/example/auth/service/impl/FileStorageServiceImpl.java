package com.example.auth.service.impl;

import com.example.auth.exception.InvalidFileException;
import com.example.auth.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Local filesystem implementation for profile image storage.
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final Path uploadDir;

    public FileStorageServiceImpl(@Value("${file.upload-dir:uploads/profiles}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException ex) {
            throw new InvalidFileException("Could not create upload directory");
        }
    }

    @Override
    public String storeProfileImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Profile image file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidFileException("Only JPEG, PNG, WEBP and GIF images are allowed");
        }

        if (file.getSize() > 2 * 1024 * 1024) {
            throw new InvalidFileException("Profile image must not exceed 2 MB");
        }

        String extension = resolveExtension(file.getOriginalFilename(), contentType);
        String filename = UUID.randomUUID() + extension;

        try {
            Path target = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/api/auth/files/profiles/" + filename;
        } catch (IOException ex) {
            throw new InvalidFileException("Failed to store profile image");
        }
    }

    public Path getUploadDir() {
        return uploadDir;
    }

    private String resolveExtension(String originalFilename, String contentType) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension != null && !extension.isBlank()) {
            return "." + extension.toLowerCase();
        }

        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
