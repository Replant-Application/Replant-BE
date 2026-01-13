package com.app.replant.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting 필터
 * IP 기반으로 API 호출 횟수 제한
 * - 일반 API: 2000 req/min (앱에서 다수의 API 호출 고려, 개발 환경 대응)
 * - 로그인/회원가입: 20 req/min
 * - 이메일 발송: 5 req/min
 * 
 * 개발 환경(dev, local 프로파일)에서는 Rate Limiting이 비활성화됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    private final Environment environment;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 개발 환경에서는 Rate Limiting 비활성화
        if (isDevelopmentEnvironment()) {
            filterChain.doFilter(request, response);
            return;
        }

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

    private boolean isDevelopmentEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        return Arrays.asList(activeProfiles).contains("dev") ||
               Arrays.asList(activeProfiles).contains("local") ||
               (activeProfiles.length == 0); // 프로파일이 없으면 개발 환경으로 간주
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
            // 일반 API: 2000 req/min (앱에서 다수의 API 호출 고려, 개발 환경 대응)
            limit = Bandwidth.classic(2000, Refill.intervally(2000, Duration.ofMinutes(1)));
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
