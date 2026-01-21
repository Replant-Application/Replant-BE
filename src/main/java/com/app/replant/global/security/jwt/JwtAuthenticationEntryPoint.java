package com.app.replant.global.security.jwt;

import com.app.replant.global.common.ApiResponse;
import com.app.replant.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        log.error("인증 실패: {}", authException.getMessage());
        
        // JwtFilter에서 설정한 ErrorCode 확인
        ErrorCode errorCode = (ErrorCode) request.getAttribute("jwtErrorCode");
        
        // ErrorCode가 없으면 기본값 (토큰 없음)
        if (errorCode == null) {
            errorCode = ErrorCode.NO_TOKEN;
        }
        
        response.setStatus(errorCode.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        // API 스펙에 맞는 에러 응답 (새로운 형식)
        ApiResponse<Void> errorResponse = ApiResponse.error(
                errorCode.getErrorCode(),
                errorCode.getErrorMsg()
        );
        
        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
