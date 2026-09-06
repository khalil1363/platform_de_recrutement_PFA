package com.daam.reclamation.dto;

import com.daam.reclamation.enumeration.ReclamationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

public final class ReclamationDtos {

    private ReclamationDtos() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReclamationRequest {
        @NotBlank
        @Size(max = 200)
        private String title;

        @NotBlank
        @Size(max = 4000)
        private String description;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReclamationUpdateRequest {
        @Size(max = 200)
        private String title;

        @Size(max = 4000)
        private String description;

        @Size(max = 4000)
        private String rhResponse;

        private ReclamationStatus status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusUpdateRequest {
        @NotNull
        private ReclamationStatus status;

        @Size(max = 4000)
        private String rhResponse;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReclamationResponse {
        private String reclamationId;
        private String title;
        private String description;
        private ReclamationStatus status;
        private String createdByUserId;
        private String createdByName;
        private String handledByUserId;
        private String handledByName;
        private String rhResponse;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
