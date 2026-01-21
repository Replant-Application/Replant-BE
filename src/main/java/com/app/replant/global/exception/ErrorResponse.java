package com.app.replant.global.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponse {
    
    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String errorCode;
    private final String message;
    private final String path;
    
    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatusCode().value())
                .error(errorCode.getStatusCode().name())
                .errorCode(errorCode.getErrorCode())
                .message(errorCode.getErrorMsg())
                .path(path)
                .build();
    }
    
    public static ErrorResponse of(HttpStatus status, String errorCode, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.name())
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .build();
    }
}
