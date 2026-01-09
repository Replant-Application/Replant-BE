package com.app.replant.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting 필터
 * IP 기반으로 API 호출 횟수 제한
 * - 일반 API: 200 req/min (앱에서 다수의 API 호출 고려)
 * - 로그인/회원가입: 20 req/min
 * - 이메일 발송: 5 req/min
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String ip = getClientIP(request);
        String uri = request.getRequestURI();

        Bucket bucket = resolveBucket(ip, uri);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {} on URI: {}", ip, uri);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"statusCode\": 429, \"message\": \"Too many requests. Please try again later.\"}");
        }
    }

    private Bucket resolveBucket(String ip, String uri) {
        String key = ip + ":" + getEndpointCategory(uri);
        return cache.computeIfAbsent(key, k -> createBucket(uri));
    }

    private Bucket createBucket(String uri) {
        Bandwidth limit;

        if (uri.contains("/api/auth/login") || uri.contains("/api/auth/join") || uri.contains("/api/auth/oauth")) {
            // 로그인/회원가입: 20 req/min
            limit = Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1)));
            log.debug("Rate limit for auth endpoint: 20 req/min");
        } else if (uri.contains("/api/auth/send-verification") || uri.contains("/api/auth/genPw")) {
            // 이메일 발송: 5 req/min
            limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
            log.debug("Rate limit for email endpoint: 5 req/min");
        } else {
            // 일반 API: 200 req/min (앱에서 다수의 API 호출 고려)
            limit = Bandwidth.classic(200, Refill.intervally(200, Duration.ofMinutes(1)));
        }

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String getEndpointCategory(String uri) {
        if (uri.contains("/api/auth/login") || uri.contains("/api/auth/join") || uri.contains("/api/auth/oauth")) {
            return "auth";
        } else if (uri.contains("/api/auth/send-verification") || uri.contains("/api/auth/genPw")) {
            return "email";
        } else {
            return "general";
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Swagger, Actuator, H2 Console은 Rate Limiting 제외
        return path.startsWith("/swagger") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/actuator") ||
                path.startsWith("/h2-console");
    }
}
