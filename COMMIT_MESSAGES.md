# 커밋 메시지 목록

## 1. 전역 공통 요소들을 global/로 이동
[refactor] #13 - 전역 공통 요소들을 global/ 패키지로 이동

- common 패키지 이동 (BaseEntity, SoftDeletableEntity, ApiResponse, PageResponse 등)
- exception 패키지 이동 (CustomException, ErrorCode, GlobalExceptionHandler)
- config 패키지 이동 (JpaConfig, SecurityConfig, RedisConfig 등)
- security 패키지 이동 (JWT, RateLimitingFilter, XssProtectionFilter)
- handler 패키지 이동 (JwtAccessDeniedHandler)
- infrastructure/service 패키지 이동 (FCM, S3, Mail, SSE, Token 서비스)
- scheduler 패키지 이동 (global/scheduler/)
- util 패키지 이동 (HtmlSanitizer)
- entity 패키지 이동 (RefreshToken)
- repository 패키지 이동 (RefreshTokenRepository)

## 2. 모든 import 경로 수정
[refactor] #13 - 레이어드 아키텍처에서 도메인형 아키텍처로 전환을 위한 import 경로 수정

- com.app.replant.common.* → com.app.replant.global.common.*
- com.app.replant.exception.* → com.app.replant.global.exception.*
- com.app.replant.service.* → com.app.replant.global.infrastructure.service.*
- com.app.replant.config.* → com.app.replant.global.config.*
- com.app.replant.jwt.* → com.app.replant.global.security.jwt.*
- com.app.replant.security.* → com.app.replant.global.security.*
- com.app.replant.handler.* → com.app.replant.global.handler.*
- CategoryType → MissionCategory로 변경 (존재하지 않는 클래스)

## 3. 중복 scheduler 파일 삭제
[refactor] #13 - 중복 scheduler 파일 삭제 및 global/scheduler로 통합

- src/main/java/com/app/replant/scheduler/* 파일 삭제
- global/scheduler/에 이미 존재하는 파일과 중복이므로 제거

## 4. 레이어드 아키텍처의 controller/dto를 도메인별로 분산
[refactor] #13 - 레이어드 아키텍처의 controller/dto를 도메인형 아키텍처로 분산

- Reant 관련 DTO → domain/reant/dto/로 이동
- User/Auth 관련 DTO → domain/user/dto/로 이동
- 공통 DTO → global/dto/로 이동
- 모든 import 경로 업데이트
- controller 폴더 삭제

## 5. AuthController를 domain/user/controller로 이동
[refactor] #13 - AuthController를 domain/user/controller로 이동

- Auth는 User 도메인의 일부이므로 domain/user/controller/로 이동
- global/infrastructure/auth/controller/에서 제거

## 6. PostRepositoryCustom에 findByUserIdAndNotDeleted 메서드 추가
[refactor] #13 - PostRepositoryCustom에 누락된 메서드 추가

- findByUserIdAndNotDeleted 메서드를 인터페이스에 추가하고 구현

## 7. User 엔티티에서 중복 인덱스 어노테이션 제거
[refactor] #13 - User 엔티티에서 중복 인덱스 어노테이션 제거

- DB에 이미 존재하는 인덱스와 충돌하므로 @Index 어노테이션 제거
- idx_user_email, idx_user_nickname, idx_user_status 인덱스는 DB에 유지
