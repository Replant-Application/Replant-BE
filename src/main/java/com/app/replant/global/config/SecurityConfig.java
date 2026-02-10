package com.app.replant.global.config;


import com.app.replant.global.handler.JwtAccessDeniedHandler;
import com.app.replant.global.security.jwt.JwtAuthenticationEntryPoint;
import com.app.replant.global.security.jwt.TokenProvider;
import com.app.replant.global.security.RateLimitingFilter;
import com.app.replant.global.security.XssProtectionFilter;
import com.app.replant.global.infrastructure.service.token.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final TokenProvider tokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final RateLimitingFilter rateLimitingFilter;
    private final XssProtectionFilter xssProtectionFilter;
    private final Environment environment;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${cors.extra.origins:}")
    private String corsExtraOrigins;

    @Value("${csp.connect-src.extras:}")
    private String cspConnectSrcExtras;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // CORS: 허용 도메인만 명시 (S5122/S1313 — 와일드카드·하드코딩 IP 제거, 설정에서 로드)
        boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev") ||
                       Arrays.asList(environment.getActiveProfiles()).contains("local");

        List<String> baseOrigins = Arrays.asList(
                frontendUrl,
                "http://localhost:8081",
                "http://localhost:3000"
        );
        List<String> extra = (corsExtraOrigins == null || corsExtraOrigins.isBlank())
                ? Collections.emptyList()
                : Arrays.stream(corsExtraOrigins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        configuration.setAllowedOriginPatterns(Stream.concat(baseOrigins.stream(), extra.stream()).collect(Collectors.toList()));
        configuration.setAllowCredentials(!isDev);

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 허용할 헤더 (보안 강화: 필요한 헤더만 명시)
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Requested-With",
            "Cache-Control",
            "Origin"  // Origin 헤더 명시적 허용
        ));

        // 응답에 노출할 헤더
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type"
        ));

        // preflight 요청 캐시 시간 (1시간)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화: JWT 기반 무상태 API이며 세션 쿠키를 사용하지 않으므로 CSRF 공격 표면이 없음.
                // 모바일/클라이언트는 Authorization 헤더로만 인증하므로 SonarQube S4502 Safe로 검토 가능.
                .csrf(AbstractHttpConfigurer::disable)
                
                // CORS 설정 활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // Exception handling 설정
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                
                // Security Headers 설정
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin) // H2 Console용
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                                        "style-src 'self' 'unsafe-inline'; " +
                                        "img-src 'self' data: https:; " +
                                        "font-src 'self' data:; " +
                                        "connect-src 'self' " + (cspConnectSrcExtras != null ? cspConnectSrcExtras + " " : "") + frontendUrl + "; " +
                                        "frame-ancestors 'self'")
                        )
                        // X-XSS-Protection header is deprecated; CSP provides better protection
                )
                
                // 세션을 사용하지 않기 때문에 STATELESS로 설정
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // 권한별 URL 접근 설정
                .authorizeHttpRequests(auth -> {
                    auth
                        // CORS Preflight 요청 허용
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        // 공개 API (인증 불필요)
                        .requestMatchers("/api/auth/**").permitAll() // 인증 관련 (OAuth 포함)
                        .requestMatchers("/auth/**").permitAll() // 기존 인증 경로
                        .requestMatchers("/api/users/restore").permitAll() // 계정 복구 (인증 불필요)
                        .requestMatchers("/api/missions/**").permitAll() // 미션 목록 조회 (공개)
                        .requestMatchers("/api/custom-missions/**").permitAll() // 커스텀 미션 목록 (공개)
                        .requestMatchers("/api/todolists/public/**").permitAll() // 공개 투두리스트 (공개)
                        .requestMatchers("/api/v1/version/**").permitAll() // 버전 체크 (인증 불필요)
                        .requestMatchers("/ws/**").permitAll() // WebSocket
                        .requestMatchers("/files/**").permitAll() // 파일 업로드/다운로드
                        // Actuator - 공개 엔드포인트만 허용 (health, info, metrics, prometheus)
                        // 순서 중요: 구체적인 경로를 먼저 설정
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/info").permitAll()
                        .requestMatchers("/actuator/metrics").permitAll()
                        .requestMatchers("/actuator/metrics/**").permitAll() // 특정 메트릭 조회도 허용
                        .requestMatchers("/actuator/prometheus").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN") // 나머지는 관리자만
                        // Swagger/OpenAPI 경로 허용 (모든 변형 포함)
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/swagger-ui/index.html").permitAll()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-resources/**", "/swagger-resources").permitAll()
                        .requestMatchers("/webjars/**").permitAll(); // Swagger UI 리소스

                    // H2 Console - 개발 환경에서만 허용
                    if (Arrays.asList(environment.getActiveProfiles()).contains("dev") ||
                        Arrays.asList(environment.getActiveProfiles()).contains("local")) {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }

                    auth

                        // 인증 필요 API
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/reant/**").authenticated()
                        .requestMatchers("/api/user-missions/**").authenticated()
                        .requestMatchers("/api/badges/**").authenticated()
                        .requestMatchers("/api/recommendations/**").authenticated()
                        .requestMatchers("/api/chat/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/api/v1/fcm/**").authenticated() // FCM 전송 API
                        .requestMatchers("/sse/**").authenticated() // SSE
                        .requestMatchers("/api/community/posts/**").authenticated() // 커뮤니티 게시글 (인증 필요)
                        .requestMatchers("/api/verifications/**").authenticated() // 인증 게시판 (인증 필요)

                        // 관리자 API (일부 개발/초기 설정용으로 공개)
                        .requestMatchers("/admin/reset-missions", "/admin/mission-count", "/admin/setup-admin").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 나머지는 인증 필요
                        .anyRequest().authenticated();
                })

                // JWT 필터 적용
                .with(new JwtSecurityConfig(tokenProvider, tokenBlacklistService), customizer -> {})

                // Rate Limiting 필터 추가 (JWT 필터 이전에 실행)
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)

                // XSS Protection 필터 추가 (가장 먼저 실행)
                .addFilterBefore(xssProtectionFilter, RateLimitingFilter.class);

        return http.build();
    }
}