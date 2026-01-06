package com.app.replant.config;


import com.app.replant.handler.JwtAccessDeniedHandler;
import com.app.replant.jwt.JwtAuthenticationEntryPoint;
import com.app.replant.jwt.TokenProvider;
import com.app.replant.security.RateLimitingFilter;
import com.app.replant.security.XssProtectionFilter;
import com.app.replant.service.token.TokenBlacklistService;
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

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 개발 환경에서는 모든 Origin 허용 (Android 에뮬레이터 지원)
        // 프로덕션에서는 특정 도메인만 허용하도록 수정 필요
        boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev") ||
                       Arrays.asList(environment.getActiveProfiles()).contains("local");
        
        if (isDev) {
            // 개발 환경: 모든 Origin 허용
            configuration.setAllowedOriginPatterns(Arrays.asList("*"));
            // 모든 Origin 허용 시 credentials는 false로 설정해야 함
            configuration.setAllowCredentials(false);
        } else {
            // 프로덕션: 특정 도메인만 허용
            configuration.setAllowedOriginPatterns(Arrays.asList(
                frontendUrl,
                "http://localhost:8081",     // React Native Metro 기본 포트
                "http://localhost:3000",     // 웹 개발 서버
                "http://127.0.0.1:8081",
                "http://127.0.0.1:3000",
                "http://10.0.2.2:*"          // Android Emulator (모든 포트)
            ));
            configuration.setAllowCredentials(true);
        }

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
                // CSRF 설정 Disable
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
                                        "connect-src 'self' http://localhost:* http://127.0.0.1:* " + frontendUrl + "; " +
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
                        .requestMatchers("/api/missions/**").permitAll() // 미션 목록 조회 (공개)
                        .requestMatchers("/api/custom-missions/**").permitAll() // 커스텀 미션 목록 (공개)
                        .requestMatchers("/api/verifications").permitAll() // 인증 게시판 목록 (공개)
                        .requestMatchers("/api/posts").permitAll() // 게시글 목록 (공개)
                        .requestMatchers("/ws/**").permitAll() // WebSocket
                        .requestMatchers("/files/**").permitAll() // 파일 업로드/다운로드
                        // Actuator - 공개 엔드포인트만 허용 (health, info)
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN") // 나머지는 관리자만
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll(); // Swagger

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
                        .requestMatchers("/sse/**").authenticated() // SSE

                        // 관리자 API (reset-missions는 개발용으로 공개)
                        .requestMatchers("/admin/reset-missions", "/admin/mission-count").permitAll()
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