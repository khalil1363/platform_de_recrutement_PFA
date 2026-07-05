package com.daam.recruitment.config;

import com.daam.recruitment.service.CvStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final CvStorageService cvStorageService;

    public WebMvcConfig(CvStorageService cvStorageService) {
        this.cvStorageService = cvStorageService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = cvStorageService.getUploadDir().toUri().toString();
        registry.addResourceHandler("/api/recruitment/files/cv/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
