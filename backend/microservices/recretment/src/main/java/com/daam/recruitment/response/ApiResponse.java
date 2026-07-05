package com.daam.recruitment.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private int status;
    private Map<String, String> errors;

    public static <T> ApiResponse<T> success(String message, T data, int status) {
        return ApiResponse.<T>builder().success(true).message(message).data(data)
                .timestamp(LocalDateTime.now()).status(status).build();
    }

    public static <T> ApiResponse<T> error(String message, int status) {
        return ApiResponse.<T>builder().success(false).message(message)
                .timestamp(LocalDateTime.now()).status(status).build();
    }
}
