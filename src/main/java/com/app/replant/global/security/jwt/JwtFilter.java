package com.app.replant.global.security.jwt;

import com.app.replant.global.exception.CustomException;
import com.app.replant.global.exception.ErrorCode;
import com.app.replant.global.infrastructure.service.token.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    // Actuator, Swagger 등 공개 엔드포인트는 JWT 필터를 거치지 않음
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && (
                path.startsWith("/actuator") ||
                path.startsWith("/swagger") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/h2-console")
        );
    }

    // 실제 필터링 로직은 doFilterInternal 에 들어감
    // JWT 토큰의 인증 정보를 현재 쓰레드의 SecurityContext 에 저장하는 역할 수행
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        try {
            // 1. Request Header, Cookie, Query Parameter에서 토큰을 꺼냄
            String jwt = resolveToken(request);

            // 2. 토큰이 있으면 검증 및 인증 정보 설정
            if (StringUtils.hasText(jwt)) {
                // 2-1. 블랙리스트 체크 (로그아웃된 토큰인지 확인)
                if (tokenBlacklistService.isBlacklisted(jwt)) {
                    log.warn("블랙리스트에 등록된 토큰 사용 시도");
                    throw new CustomException(ErrorCode.INVALID_TOKEN);
                }
                
                // 2-2. 토큰 검증 (만료, 서명 등)
                tokenProvider.validateToken(jwt); // 실패 시 CustomException 던짐
                
                // 2-3. 성공하면 인증 정보 설정
                Authentication authentication = tokenProvider.getAuthentication(jwt);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            // 토큰이 없으면 그냥 통과 (SecurityConfig에서 인증 필요 여부 판단)
        } catch (CustomException e) {
            // TokenProvider에서 던진 CustomException (TOKEN_EXPIRED, INVALID_TOKEN)
            log.error("JWT 검증 실패: {} - {}", e.getErrorCode().name(), e.getMessage());
            SecurityContextHolder.clearContext();

            // SSE 경로인 경우 SSE 형식으로 직접 에러 응답
            String requestUri = request.getRequestURI();
            if (requestUri != null && requestUri.startsWith("/sse/")) {
                sendSseErrorResponse(response, e.getErrorCode());
                return; // filterChain 중단
            }

            // 일반 API는 request attribute에 저장하고 계속 진행
            // EntryPoint에서 이 attribute를 읽어서 처리
            request.setAttribute("jwtErrorCode", e.getErrorCode());
        } catch (Exception e) {
            // 기타 예외
            log.error("JWT 필터 처리 중 예외 발생: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();

            // SSE 경로인 경우 SSE 형식으로 직접 에러 응답
            String requestUri = request.getRequestURI();
            if (requestUri != null && requestUri.startsWith("/sse/")) {
                sendSseErrorResponse(response, ErrorCode.INVALID_TOKEN);
                return; // filterChain 중단
            }

            // 일반 API는 request attribute에 저장하고 계속 진행
            request.setAttribute("jwtErrorCode", ErrorCode.INVALID_TOKEN);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Request Header, Cookie, Query Parameter에서 토큰 정보를 꺼내오기
     * 우선순위: Authorization 헤더 > Cookie > Query Parameter
     * (보안상 Cookie를 Query보다 우선시)
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. Authorization 헤더에서 토큰 확인 (가장 안전)
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }

        // 2. Cookie에서 토큰 확인 (SSE용 - HttpOnly, Secure 권장)
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 3. Query Parameter에서 토큰 확인 (SSE용 - EventSource는 헤더를 지원하지 않음)
        // 보안상 최소화 권장: 로그/히스토리/리퍼러로 노출 위험
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }

        return null;
    }

    /**
     * SSE 경로에서 인증 실패 시 SSE 형식으로 에러 응답
     * 응답을 쓴 후에는 filterChain을 진행하지 않음
     */
    private void sendSseErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        // 이미 응답이 commit되었으면 처리하지 않음
        if (response.isCommitted()) {
            log.warn("SSE 에러 응답 실패: 응답이 이미 commit됨");
            return;
        }

        response.setStatus(errorCode.getStatusCode().value());
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        // SSE 형식으로 에러 메시지 전송
        String errorMessage = String.format("event: error\ndata: {\"code\":\"%s\",\"message\":\"%s\"}\n\n",
                errorCode.getErrorCode(), errorCode.getErrorMsg());

        try {
            response.getWriter().write(errorMessage);
            response.getWriter().flush();
        } catch (IOException e) {
            log.error("SSE 에러 응답 전송 실패", e);
            throw e;
        }
    }
}
