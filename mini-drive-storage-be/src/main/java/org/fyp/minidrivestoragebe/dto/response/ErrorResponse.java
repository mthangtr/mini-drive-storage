package org.fyp.minidrivestoragebe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;
    private Long timestamp;
    private Map<String, List<String>> validationErrors;

    public static ErrorResponse of(int status, String error, String message, String path) {
        return ErrorResponse.builder()
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static ErrorResponse withValidation(int status, String error, String message, 
                                               String path, Map<String, List<String>> validationErrors) {
        return ErrorResponse.builder()
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .timestamp(System.currentTimeMillis())
                .validationErrors(validationErrors)
                .build();
    }
}
