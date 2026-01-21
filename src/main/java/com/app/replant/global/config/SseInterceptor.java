package com.app.replant.global.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class SseInterceptor implements HandlerInterceptor {
    
    @Value("${FRONTEND_URL}")
    private String frontendUrl;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // SSE 경로인 경우 헤더 설정
        if (request.getRequestURI().startsWith("/sse/")) {
            // SSE 응답 헤더 설정
            response.setHeader("Content-Type", MediaType.TEXT_EVENT_STREAM_VALUE);
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");
            response.setHeader("X-Accel-Buffering", "no"); // Nginx 버퍼링 방지
            
            // CORS 헤더 설정
            response.setHeader("Access-Control-Allow-Origin", frontendUrl);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setHeader("Access-Control-Expose-Headers", "*");
            
            log.debug("SSE 인터셉터: 헤더 설정 완료 - URI: {}", request.getRequestURI());
        }
        return true;
    }
}

