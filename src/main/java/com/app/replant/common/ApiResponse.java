package com.app.replant.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 표준화된 API 응답 래퍼
 *
 * HTTP 상태 코드는 HTTP 헤더로만 전달하며, 응답 본문에는 포함하지 않습니다.
 * 성공 응답: { data, message(optional) }
 * 에러 응답: { error: { code, message, fields(optional) } }
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    // 성공 시 데이터
    private T data;

    // 선택적 메시지 (성공 시에만 사용)
    private String message;

    // 에러 상세 정보 (실패 시에만 존재)
    private ErrorDetails error;

    // ========== DEPRECATED FIELDS (하위 호환성 유지) ==========
    @Deprecated
    private Integer code;

    @Deprecated
    private Boolean success;

    // ========== NEW API (권장) ==========

    /**
     * 성공 응답 생성 (데이터만)
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .data(data)
                .build();
    }

    /**
     * 성공 응답 생성 (데이터 + 메시지)
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .data(data)
                .message(message)
                .build();
    }

    /**
     * 에러 응답 생성
     */
    public static <T> ApiResponse<T> error(String errorCode, String errorMessage) {
        return ApiResponse.<T>builder()
                .error(ErrorDetails.builder()
                        .code(errorCode)
                        .message(errorMessage)
                        .build())
                .build();
    }

    /**
     * 에러 응답 생성 (필드 검증 에러 포함)
     */
    public static <T> ApiResponse<T> error(String errorCode, String errorMessage, List<FieldError> fields) {
        return ApiResponse.<T>builder()
                .error(ErrorDetails.builder()
                        .code(errorCode)
                        .message(errorMessage)
                        .fields(fields)
                        .build())
                .build();
    }

    // ========== DEPRECATED METHODS (하위 호환성 유지) ==========

    /**
     * @deprecated HTTP 상태 코드는 HTTP 헤더로만 전달해야 합니다. success(T data) 사용을 권장합니다.
     */
    @Deprecated
    public static <T> ApiResponse<T> res(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .success(code >= 200 && code < 300)
                .message(message)
                .build();
    }

    /**
     * @deprecated HTTP 상태 코드는 HTTP 헤더로만 전달해야 합니다. success(T data, String message) 사용을 권장합니다.
     */
    @Deprecated
    public static <T> ApiResponse<T> res(int code, String message, T data) {
        return ApiResponse.<T>builder()
                .code(code)
                .success(code >= 200 && code < 300)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * @deprecated error(String errorCode, String errorMessage) 사용을 권장합니다.
     */
    @Deprecated
    public static <T> ApiResponse<T> error(String error) {
        return ApiResponse.<T>builder()
                .code(500)
                .success(false)
                .error(ErrorDetails.builder()
                        .code("INTERNAL_SERVER_ERROR")
                        .message(error)
                        .build())
                .build();
    }

    /**
     * @deprecated error(String errorCode, String errorMessage) 사용을 권장합니다.
     */
    @Deprecated
    public static <T> ApiResponse<T> error(int code, String error) {
        return ApiResponse.<T>builder()
                .code(code)
                .success(false)
                .error(ErrorDetails.builder()
                        .code("ERROR")
                        .message(error)
                        .build())
                .build();
    }

    // ========== NESTED CLASSES ==========

    /**
     * 에러 상세 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetails {
        /**
         * 에러 코드 (예: USER_NOT_FOUND, VALIDATION_ERROR)
         */
        private String code;

        /**
         * 에러 메시지 (사용자에게 표시할 수 있는 메시지)
         */
        private String message;

        /**
         * 필드 검증 에러 목록 (선택적)
         */
        private List<FieldError> fields;
    }

    /**
     * 필드 검증 에러
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        /**
         * 에러가 발생한 필드명
         */
        private String field;

        /**
         * 필드 에러 메시지
         */
        private String message;
    }
}
