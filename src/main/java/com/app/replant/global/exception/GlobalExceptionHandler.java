package com.app.replant.global.exception;

import com.app.replant.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리기
 *
 * 애플리케이션 전체에서 발생하는 예외를 일관된 형식으로 처리합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        /**
         * CustomException 처리
         */
        @ExceptionHandler(CustomException.class)
        public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
                log.error("CustomException: {} - {}", e.getErrorCode().getErrorCode(), e.getMessage(), e);

                return ResponseEntity
                                .status(e.getErrorCode().getStatusCode())
                                .body(ApiResponse.error(
                                                e.getErrorCode().getErrorCode(),
                                                e.getMessage() != null ? e.getMessage()
                                                                : e.getErrorCode().getErrorMsg()));
        }

        /**
         * Validation 예외 처리 (@Valid, @Validated 검증 실패)
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
                        MethodArgumentNotValidException e) {
                log.error("Validation error: {}", e.getMessage());

                List<ApiResponse.FieldError> fieldErrors = e.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(err -> ApiResponse.FieldError.builder()
                                                .field(err.getField())
                                                .message(err.getDefaultMessage())
                                                .build())
                                .collect(Collectors.toList());

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(
                                                "VALIDATION_ERROR",
                                                "입력값 검증에 실패했습니다",
                                                fieldErrors));
        }

        /**
         * BindException 처리 (모델 바인딩 실패)
         */
        @ExceptionHandler(BindException.class)
        public ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
                log.error("Binding error: {}", e.getMessage());

                List<ApiResponse.FieldError> fieldErrors = e.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(err -> ApiResponse.FieldError.builder()
                                                .field(err.getField())
                                                .message(err.getDefaultMessage())
                                                .build())
                                .collect(Collectors.toList());

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(
                                                "VALIDATION_ERROR",
                                                "입력값 검증에 실패했습니다",
                                                fieldErrors));
        }

        /**
         * 필수 파라미터 누락 예외 처리
         */
        @ExceptionHandler(MissingServletRequestParameterException.class)
        public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
                        MissingServletRequestParameterException e) {
                log.error("Missing parameter: {}", e.getMessage());

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(
                                                "MISSING_PARAMETER",
                                                String.format("필수 파라미터 '%s'가 누락되었습니다", e.getParameterName())));
        }

        /**
         * 파라미터 타입 불일치 예외 처리
         */
        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
                        MethodArgumentTypeMismatchException e) {
                log.error("Type mismatch: {}", e.getMessage());

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(
                                                "INVALID_FORMAT",
                                                String.format("파라미터 '%s'의 형식이 올바르지 않습니다", e.getName())));
        }

        /**
         * HTTP 메시지를 읽을 수 없는 경우 (잘못된 JSON 등)
         */
        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
                        HttpMessageNotReadableException e) {
                log.error("Message not readable: {}", e.getMessage());

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(
                                                "INVALID_FORMAT",
                                                "요청 본문의 형식이 올바르지 않습니다"));
        }

        /**
         * 지원하지 않는 HTTP 메서드 예외 처리
         */
        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
                        HttpRequestMethodNotSupportedException e) {
                log.error("Method not supported: {}", e.getMessage());

                return ResponseEntity
                                .status(HttpStatus.METHOD_NOT_ALLOWED)
                                .body(ApiResponse.error(
                                                "METHOD_NOT_ALLOWED",
                                                String.format("'%s' 메서드는 지원되지 않습니다. 지원되는 메서드: %s",
                                                                e.getMethod(),
                                                                String.join(", ",
                                                                                e.getSupportedMethods() != null ? e
                                                                                                .getSupportedMethods()
                                                                                                : new String[0]))));
        }

        /**
         * 핸들러를 찾을 수 없는 경우 (404)
         */
        @ExceptionHandler(NoHandlerFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleNoHandlerFoundException(NoHandlerFoundException e) {
                log.error("No handler found: {}", e.getMessage());

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(
                                                "RESOURCE_NOT_FOUND",
                                                "요청한 리소스를 찾을 수 없습니다"));
        }

        /**
         * 처리되지 않은 모든 예외
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
                log.error("Unhandled exception: {}", e.getMessage(), e);

                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.error(
                                                "INTERNAL_SERVER_ERROR",
                                                "서버 오류가 발생했습니다"));
        }
}
