package com.example.auth.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Service contract for storing uploaded files.
 */
public interface FileStorageService {

    /**
     * Stores a profile image and returns its public URL path.
     *
     * @param file the uploaded image file
     * @return public URL path for the stored image
     */
    String storeProfileImage(MultipartFile file);
}
