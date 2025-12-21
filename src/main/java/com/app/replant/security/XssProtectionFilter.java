package com.app.replant.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * XSS 방어 필터
 * 요청 파라미터와 헤더의 HTML 태그를 이스케이프 처리
 *
 * 주의: 이 필터는 URL 파라미터와 헤더만 보호합니다.
 * JSON 요청 본문(Request Body)의 XSS 방어는 다음 방법을 사용하세요:
 * 1. @Valid 어노테이션과 DTO 검증 사용
 * 2. HtmlSanitizer 유틸리티를 서비스 레이어에서 사용
 * 3. 프론트엔드에서 Content Security Policy (CSP) 헤더 적용
 *
 * @see com.app.replant.util.HtmlSanitizer
 */
@Slf4j
@Component
public class XssProtectionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        XssProtectionRequestWrapper wrappedRequest = new XssProtectionRequestWrapper(request);
        filterChain.doFilter(wrappedRequest, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // File upload/download는 XSS 필터 제외
        return path.startsWith("/files/") ||
                path.startsWith("/swagger") ||
                path.startsWith("/v3/api-docs");
    }

    private static class XssProtectionRequestWrapper extends HttpServletRequestWrapper {

        public XssProtectionRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String[] getParameterValues(String parameter) {
            String[] values = super.getParameterValues(parameter);
            if (values == null) {
                return null;
            }

            int count = values.length;
            String[] encodedValues = new String[count];
            for (int i = 0; i < count; i++) {
                encodedValues[i] = stripXSS(values[i]);
            }

            return encodedValues;
        }

        @Override
        public String getParameter(String parameter) {
            String value = super.getParameter(parameter);
            return stripXSS(value);
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            return stripXSS(value);
        }

        private String stripXSS(String value) {
            if (value == null) {
                return null;
            }

            // HTML 엔티티로 변환
            value = HtmlUtils.htmlEscape(value);

            // 추가 XSS 패턴 제거
            value = value.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
            value = value.replaceAll("\\(", "&#40;").replaceAll("\\)", "&#41;");
            value = value.replaceAll("'", "&#39;");
            value = value.replaceAll("eval\\((.*)\\)", "");
            value = value.replaceAll("[\\\"\\\'][\\s]*javascript:(.*)[\\\"\\\']", "\"\"");
            value = value.replaceAll("script", "");

            return value;
        }
    }
}
