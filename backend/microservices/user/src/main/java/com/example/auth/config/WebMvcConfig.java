package com.example.auth.config;

import com.example.auth.service.impl.FileStorageServiceImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves uploaded profile images as static resources.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final FileStorageServiceImpl fileStorageService;

    public WebMvcConfig(FileStorageServiceImpl fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = fileStorageService.getUploadDir().toUri().toString();
        registry.addResourceHandler("/api/auth/files/profiles/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
