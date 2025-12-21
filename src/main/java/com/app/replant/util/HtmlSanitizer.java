package com.app.replant.util;

import org.springframework.web.util.HtmlUtils;

/**
 * HTML 콘텐츠 정제 유틸리티
 * 사용자가 입력한 HTML을 안전하게 정제하여 XSS 공격을 방어합니다.
 */
public class HtmlSanitizer {

    /**
     * 기본 HTML 이스케이프 처리
     * 모든 HTML 태그와 특수문자를 엔티티로 변환합니다.
     */
    public static String sanitize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        // Spring의 HtmlUtils를 사용한 기본 이스케이프
        String sanitized = HtmlUtils.htmlEscape(input);

        // 추가 XSS 패턴 제거
        sanitized = sanitized.replaceAll("(?i)<script.*?>.*?</script.*?>", "");
        sanitized = sanitized.replaceAll("(?i)<iframe.*?>.*?</iframe.*?>", "");
        sanitized = sanitized.replaceAll("(?i)<object.*?>.*?</object.*?>", "");
        sanitized = sanitized.replaceAll("(?i)<embed.*?>.*?</embed.*?>", "");
        sanitized = sanitized.replaceAll("(?i)on\\w+\\s*=", ""); // onclick, onerror 등
        sanitized = sanitized.replaceAll("(?i)javascript:", "");
        sanitized = sanitized.replaceAll("(?i)vbscript:", "");
        sanitized = sanitized.replaceAll("(?i)data:text/html", "");

        return sanitized;
    }

    /**
     * 여러 문자열을 한번에 정제
     */
    public static String[] sanitizeAll(String... inputs) {
        if (inputs == null) {
            return null;
        }

        String[] sanitized = new String[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            sanitized[i] = sanitize(inputs[i]);
        }
        return sanitized;
    }

    /**
     * 안전한 HTML 허용 (제한적인 태그만 허용)
     * 게시글 본문 등에서 기본적인 서식을 허용해야 하는 경우 사용
     */
    public static String sanitizeAllowBasicHtml(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        // 위험한 태그와 속성 제거
        String sanitized = input;

        // Script 관련 완전 제거
        sanitized = sanitized.replaceAll("(?i)<script.*?>.*?</script.*?>", "");
        sanitized = sanitized.replaceAll("(?i)<iframe.*?>.*?</iframe.*?>", "");
        sanitized = sanitized.replaceAll("(?i)<object.*?>.*?</object.*?>", "");
        sanitized = sanitized.replaceAll("(?i)<embed.*?>.*?</embed.*?>", "");

        // 이벤트 핸들러 제거
        sanitized = sanitized.replaceAll("(?i)on\\w+\\s*=\\s*[\"'][^\"']*[\"']", "");
        sanitized = sanitized.replaceAll("(?i)on\\w+\\s*=\\s*\\S+", "");

        // 위험한 프로토콜 제거
        sanitized = sanitized.replaceAll("(?i)javascript:", "");
        sanitized = sanitized.replaceAll("(?i)vbscript:", "");
        sanitized = sanitized.replaceAll("(?i)data:text/html", "");

        // style 태그 제거 (CSS injection 방지)
        sanitized = sanitized.replaceAll("(?i)<style.*?>.*?</style.*?>", "");

        // 허용되지 않는 태그는 모두 제거 (화이트리스트 방식)
        // 허용: p, br, b, i, u, strong, em, ul, ol, li, a (href만), img (src, alt만)
        sanitized = removeUnallowedTags(sanitized);

        return sanitized;
    }

    /**
     * 허용되지 않은 HTML 태그 제거 (화이트리스트 방식)
     */
    private static String removeUnallowedTags(String html) {
        // 간단한 구현 - 실제 프로덕션에서는 OWASP Java HTML Sanitizer 라이브러리 사용 권장
        String[] allowedTags = {"p", "br", "b", "i", "u", "strong", "em", "ul", "ol", "li", "a", "img", "h1", "h2", "h3", "h4", "h5", "h6"};

        // 허용되지 않는 모든 태그를 제거
        String result = html.replaceAll("<(?!/?(" + String.join("|", allowedTags) + ")\\b)[^>]+>", "");

        // a 태그에서 href 외의 속성 제거
        result = result.replaceAll("(<a\\s+)([^>]*?)(href\\s*=\\s*[\"'][^\"']*[\"'])([^>]*?>)", "$1$3>");

        // img 태그에서 src, alt 외의 속성 제거
        result = result.replaceAll("(<img\\s+)([^>]*?)(src\\s*=\\s*[\"'][^\"']*[\"'])([^>]*?)(alt\\s*=\\s*[\"'][^\"']*[\"'])?([^>]*?>)", "$1$3 $5>");

        return result;
    }

    /**
     * SQL Injection 방지를 위한 문자열 정제
     * 주의: PreparedStatement 사용이 우선이며, 이는 추가 방어선입니다.
     */
    public static String sanitizeForSql(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        // 위험한 SQL 키워드 제거 (대소문자 무관)
        String sanitized = input;
        sanitized = sanitized.replaceAll("(?i)(--|;|/\\*|\\*/|xp_|sp_|exec|execute|select|insert|update|delete|drop|create|alter|union|or|and)", "");
        sanitized = sanitized.replaceAll("['\"\\\\]", ""); // 따옴표와 백슬래시 제거

        return sanitized;
    }
}
