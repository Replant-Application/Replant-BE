package com.app.replant.jwt;

import com.app.replant.controller.dto.TokenDto;
import com.app.replant.domain.user.security.UserDetail;
import com.app.replant.domain.user.security.UserDetailService;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TokenProvider {

    private static final String AUTHORITIES_KEY = "auth";
    private static final String BEARER_TYPE = "Bearer";
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24;       // 24시간 (모바일 앱용)
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000L * 60 * 60 * 24 * 30; // 30일 (모바일 앱용)
    private final UserDetailService userDetailService;
    private final Key key;

    public TokenProvider(@Value("${jwt.secret}") String secretKey, UserDetailService userDetailService) {
        this.userDetailService = userDetailService;
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }


    public TokenDto generateTokenDto(org.springframework.security.core.userdetails.UserDetails principal) {

        // 권한들 가져오기
        String authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();

        // Access Token 생성
        Date accessTokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);
        String accessToken = Jwts.builder()
                .setSubject(principal.getUsername())       // payload "sub": "email"
                .claim(AUTHORITIES_KEY, authorities)        // payload "auth": "ROLE_USER"
                .setExpiration(accessTokenExpiresIn)        // payload "exp": 1516239022 (예시)
                .signWith(key)                             // header "alg": "HS512" (Key 타입에서 자동 감지)
                .compact();

        // Refresh Token 생성
        String refreshToken = Jwts.builder()
                .setExpiration(new Date(now + REFRESH_TOKEN_EXPIRE_TIME))
                .signWith(key)                              // Key 타입에서 자동으로 알고리즘 감지
                .compact();

        return TokenDto.builder()
                .grantType(BEARER_TYPE)
                .accessToken(accessToken)
                .accessTokenExpiresIn(accessTokenExpiresIn.getTime())
                .refreshToken(refreshToken)
                .build();
    }

    public Authentication getAuthentication(String accessToken) {
        // 토큰 복호화
        Claims claims = parseClaims(accessToken);

        if (claims.get(AUTHORITIES_KEY) == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // 토큰에 담긴 사용자의 이메일 가져오기
        String userEmail = claims.getSubject();

        // 이메일을 기반으로 사용자 정보 조회
        UserDetail principal = userDetailService.loadUserByUsername(userEmail);

        return new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities());
    }




    /**
     * 토큰 검증 - 실패 시 바로 예외 던짐
     * @throws CustomException 토큰이 유효하지 않은 경우
     */
    public void validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) key)
                    .build()
                    .parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }
    
    /**
     * 토큰 검증 (boolean 반환) - Filter에서 사용
     */
    public boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (CustomException e) {
            return false;
        }
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) key)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (JwtException e) {
            // 서명 오류, 형식 오류 등
            log.error("토큰 파싱 실패: {}", e.getMessage());
            throw e;  // JwtFilter에서 잡히도록 다시 던짐
        }
    }

    /**
     * 토큰의 남은 유효기간을 초 단위로 계산
     * @param token JWT 토큰
     * @return 남은 유효기간 (초 단위), 만료되었거나 파싱 실패 시 0 반환
     */
    public long getRemainingExpirationTime(String token) {
        try {
            Claims claims = parseClaims(token);
            Date expiration = claims.getExpiration();
            if (expiration == null) {
                return 0;
            }
            long remainingTime = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            return Math.max(0, remainingTime);
        } catch (Exception e) {
            log.debug("토큰 만료 시간 계산 실패: {}", e.getMessage());
            return 0;
        }
    }
}