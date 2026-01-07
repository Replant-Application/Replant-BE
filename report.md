# Replant API 명세서

## 인증 (Auth)

| 이름 | 메서드 | 엔드포인트 | 설명 | 비고 | 제작여부 |
|------|--------|------------|------|------|----------|
| 회원가입 | POST | /api/auth/join | 새로운 회원 등록 및 자동 로그인 | - | O |
| 로그인 | POST | /api/auth/login | 이메일/비밀번호로 JWT 토큰 발급 | - | O |
| ID 중복 체크 | GET | /api/auth/idCheck | 이메일 중복 여부 확인 | ?memberId=이메일 | O |
| 토큰 재발급 | POST | /api/auth/refresh | Refresh Token으로 Access Token 재발급 | - | O |
| 현재 로그인 사용자 정보 | GET | /api/auth/user | JWT 토큰으로 현재 사용자 정보 조회 | - | O |
| ID 찾기 | GET | /api/auth/searchId | 닉네임/전화번호로 이메일 찾기 | - | O |
| 이메일 인증번호 발송 | POST | /api/auth/send-verification | 회원가입용 인증번호 이메일 발송 | - | O |
| 이메일 인증번호 확인 | POST | /api/auth/verify-email | 인증번호 검증 | - | O |
| 로그아웃 | POST | /api/auth/logout | 토큰 블랙리스트 등록 및 로그아웃 | - | O |
| 비밀번호 재설정 (임시) | PATCH | /api/auth/genPw | 임시 비밀번호 이메일 발송 | - | O |
| 비밀번호 변경 | PATCH | /api/auth/resetPw | 기존 비밀번호 확인 후 변경 | - | O |
| OAuth 로그인 | POST | /api/auth/oauth/{provider} | 소셜 로그인 (KAKAO, GOOGLE, NAVER, APPLE) | - | O |

---

## 사용자 (User)

| 이름 | 메서드 | 엔드포인트 | 설명 | 비고 | 제작여부 |
|------|--------|------------|------|------|----------|
| 내 정보 조회 | GET | /api/users/me | 로그인한 사용자 정보 조회 | - | O |
| 내 정보 수정 | PUT | /api/users/me | 로그인한 사용자 정보 수정 | - | O |
| 다른 유저 프로필 조회 | GET | /api/users/{userId} | 다른 사용자 프로필 조회 | 리안트 정보 포함 | O |

---

## 회원 (Member)

| 이름 | 메서드 | 엔드포인트 | 설명 | 비고 | 제작여부 |
|------|--------|------------|------|------|----------|
| 필수 카테고리 설정 | POST | /member/essential-categories | 필수 카테고리 설정 | 레거시 API | O |

---

## 리안트 (Reant) - 펫

| 이름 | 메서드 | 엔드포인트 | 설명 | 비고 | 제작여부 |
|------|--------|------------|------|------|----------|
| 내 펫 조회 | GET | /api/reant | 내 펫 정보 조회 | - | O |
| 펫 정보 수정 | PUT | /api/reant | 펫 이름 등 정보 수정 | - | O |
| 펫 상태 조회 | GET | /api/reant/status | 기분, 건강, 배고픔 등 상세 상태 | - | O |
| 펫 먹이주기 | POST | /api/reant/feed | 배고픔 -30, 건강도 +5, 기분 +10 | - | O |
| 펫 쉬게하기 | POST | /api/reant/rest | 건강도 +20, 기분 +10 | - | O |
| 펫과 놀아주기 | POST | /api/reant/play | 기분 +20, 배고픔 +5 | - | O |
| 펫 쓰다듬기 | POST | /api/reant/pet | 기분 +15 | - | O |

---

## 미션 (Mission) - 시스템 미션

| 이름 | 메서드 | 엔드포인트 | 설명 | 비고 | 제작여부 |
|------|--------|------------|------|------|----------|
| 미션 목록 조회 | GET | /api/missions | 시스템 미션 목록 조회 | ?type=&verificationType= | O |
| 미션 목록 조회 (필터링) | GET | /api/missions/filtered | 사용자 맞춤 미션 필터링 | worryType, ageRange, genderType 등 | O |
| 미션 상세 조회 | GET | /api/missions/{missionId} | 특정 미션 상세 정보 | - | O |
| 미션 리뷰 목록 조회 | GET | /api/missions/{missionId}/reviews | 미션 리뷰 목록 | 페이징 | O |
| 미션 리뷰 작성 | POST | /api/missions/{missionId}/reviews | 미션 리뷰 작성 | - | O |
| 미션 QnA 목록 조회 | GET | /api/missions/{missionId}/qna | 미션 QnA 목록 | 페이징 | O |
| 미션 QnA 상세 조회 | GET | /api/missions/{missionId}/qna/{qnaId} | 특정 QnA 상세 | - | O |
| 미션 QnA 질문 작성 | POST | /api/missions/{missionId}/qna | 질문 작성 | - | O |
| 미션 QnA 답변 작성 | POST | /api/missions/{missionId}/qna/{qnaId}/answers | 답변 작성 | - | O |
| 미션 QnA 답변 채택 | PUT | /api/missions/{missionId}/qna/{qnaId}/answers/{answerId}/accept | 답변 채택 | 질문자만 가능 | O |

---

## 내 미션 (UserMission)

| 이름 | 메서드 | 엔드포인트 | 설명 | 비고 | 제작여부 |
|------|--------|------------|------|------|----------|
| 내 미션 목록 조회 | GET | /api/missions/my | 할당된 미션 목록 | ?status=&missionType= | O |
| 내 미션 상세 조회 | GET | /api/missions/my/{userMissionId} | 특정 미션 상세 | - | O |
| 시스템 미션 추가 | POST | /api/missions/my | 시스템 미션을 내 미션에 추가 | missionId | O |
| 커스텀 미션 추가 | POST | /api/missions/my/custom | 커스텀 미션을 내 미션에 추가 | customMissionId | O |
| 미션 인증 (GPS/TIME) | POST | /api/missions/my/{userMissionId}/verify | GPS 또는 시간으로 미션 인증 | - | O |
| 미션 수행 이력 조회 | GET | /api/missions/my/history | 완료/실패 포함 전체 이력 | - | O |
| 기상 미션 시간대 설정 | POST | /api/missions/my/wakeup/settings | 다음 주차 기상 미션 시간대 설정 | 6-8시/8-10시/10-12시 | O |
| 기상 미션 시간대 수정 | PUT | /api/missions/my/wakeup/settings/{settingId} | 기상 미션 시간대 수정 | - | O |
| 현재 주차 기상 설정 조회 | GET | /api/missions/my/wakeup/settings/current | 현재 주차 설정 조회 | - | O |
| 특정 주차 기상 설정 조회 | GET | /api/missions/my/wakeup/settings | 특정 주차 설정 조회 | ?weekNumber=&year= | O |
| 다음 주차 설정 정보 | GET | /api/missions/my/wakeup/settings/next-week-info | 다음 주차 설정 정보 확인 | - | O |
| 기상 인증 시간 확인 | GET | /api/missions/my/wakeup/verify-time | 현재 시간이 기상 인증 시간대인지 확인 | - | O |

---

## 커스텀 미션 (CustomMission)

| 이름 | 메서드 | 엔드포인트 | 설명 | 비고 | 제작여부 |
|------|--------|------------|------|------|----------|
| 커스텀 미션 목록 조회 | GET | /api/missions/custom | 공개된 커스텀 미션 목록 | ?verificationType= | O |
| 커스텀 미션 상세 조회 | GET | /api/missions/custom/{customMissionId} | 커스텀 미션 상세 | - | O |
| 커스텀 미션 생성 | POST | /api/missions/custom | 새 커스텀 미션 생성 | - | O |
| 커스텀 미션 수정 | PUT | /api/missions/custom/{customMissionId} | 커스텀 미션 수정 | 생성자만 가능 | O |
| 커스텀 미션 삭제 | DELETE | /api/missions/custom/{customMissionId} | 커스텀 미션 삭제 | 생성자만 가능 | O |

---

## 미션 인증 (Verification)

| 이름 | 메서드 | 엔드포인트 | 설명 | 비고 | 제작여부 |
|------|--------|------------|------|------|----------|
| 인증글 목록 조회 | GET | /api/verifications | 인증글 목록 | ?status=&missionId=&customMissionId= | O |
| 인증글 상세 조회 | GET | /api/verifications/{verificationId} | 인증글 상세 | - | O |
| 인증글 작성 | POST | /api/verifications | COMMUNITY 타입 인증글 작성 | - | O |
| 인증글 수정 | PUT | /api/verifications/{verificationId} | 인증글 수정 | - | O |
| 인증글 삭제 | DELETE | /api/verifications/{verificationId} | 인증글 삭제 | - | O |
| 인증 투표 | POST | /api/verifications/{verificationId}/votes | 좋아요/싫어요 투표 | - | O |
| GPS 인증 | POST | /api/verifications/gps | GPS 타입 미션 인증 | latitude, longitude | O |
| 시간 인증 | POST | /api/verifications/time | TIME 타입 미션 인증 | startedAt, endedAt | O |
| 인증글 댓글 목록 조회 | GET | /api/verifications/{verificationId}/comments | 댓글 목록 | 페이징 | O |
| 인증글 댓글 작성 | POST | /api/verifications/{verificationId}/comments | 댓글 작성 | 대댓글 지원 | O |
| 인증글 댓글 수정 | PUT | /api/verifications/{verificationId}/comments/{commentId} | 댓글 수정 | - | O |
| 인증글 댓글 삭제 | DELETE | /api/verifications/{verificationId}/comments/{commentId} | 댓글 삭제 | - | O |
| 인증글 댓글 수 조회 | GET | /api/verifications/{verificationId}/comments/count | 댓글 수 | - | O |

---

## 다이어리 (Diary)

| 이름 | 메서드 | 엔드포인트 | 설명 | 비고 | 제작여부 |
|------|--------|------------|------|------|----------|
| 다이어리 목록 조회 | GET | /api/diaries | 다이어리 목록 | 페이징 | O |
| 다이어리 상세 조회 | GET | /api/diaries/{diaryId} | 다이어리 상세 | - | O |
| 날짜별 다이어리 조회 | GET | /api/diaries/by-date | 특정 날짜 다이어리 | ?date=YYYY-MM-DD | O |
| 기간별 다이어리 조회 | GET | /api/diaries/range | 기간별 다이어리 목록 | ?startDate=&endDate= | O |
| 다이어리 생성 | POST | /api/diaries | 다이어리 생성 | - | O |
| 다이어리 수정 | PUT | /api/diaries/{diaryId} | 다이어리 수정 | - | O |
| 다이어리 삭제 | DELETE | /api/diaries/{diaryId} | 다이어리 삭제 | - | O |
| 다이어리 통계 조회 | GET | /api/diaries/stats | 다이어리 통계 | - | O |

---

## 커뮤니티 게시판 (Post)

| 이름 | 메서드 | 엔드포인트 | 설명 | 비고 | 제작여부 |
|------|--------|------------|------|------|----------|
| 게시글 목록 조회 | GET | /api/community/posts | 게시글 목록 | ?missionId=&customMissionId=&badgeOnly= | O |
| 게시글 상세 조회 | GET | /api/community/posts/{postId} | 게시글 상세 | - | O |
| 게시글 작성 | POST | /api/community/posts | 게시글 작성 | - | O |
| 게시글 수정 | PUT | /api/community/posts/{postId} | 게시글 수정 | - | O |
| 게시글 삭제 | DELETE | /api/community/posts/{postId} | 게시글 삭제 | - | O |
| 댓글 목록 조회 | GET | /api/community/posts/{postId}/comments | 댓글 목록 | 페이징 | O |
| 댓글 작성 | POST | /api/community/posts/{postId}/comments | 댓글 작성 | 대댓글 지원 | O |
| 댓글 수정 | PUT | /api/community/posts/{postId}/comments/{commentId} | 댓글 수정 | - | O |
| 댓글 삭제 | DELETE | /api/community/posts/{postId}/comments/{commentId} | 댓글 삭제 | - | O |

---

## 뱃지 (Badge)

| 이름 | 메서드 | 엔드포인트 | 설명 | 비고 | 제작여부 |
|------|--------|------------|------|------|----------|
| 내 유효 뱃지 목록 조회 | GET | /api/badges | 현재 유효한 뱃지 목록 | - | O |
| 뱃지 히스토리 조회 | GET | /api/badges/history | 만료된 뱃지 포함 전체 이력 | 페이징 | O |

---

## 알림 (Notification)

| 이름 | 메서드 | 엔드포인트 | 설명 | 비고 | 제작여부 |
|------|--------|------------|------|------|----------|
| 알림 목록 조회 | GET | /api/notifications | 알림 목록 + 읽지 않은 수 | ?isRead= | O |
| 알림 읽음 처리 | PUT | /api/notifications/{notificationId}/read | 특정 알림 읽음 처리 | - | O |
| 전체 알림 읽음 처리 | PUT | /api/notifications/read-all | 모든 알림 읽음 처리 | - | O |

---

## 채팅 (Chat)

| 이름 | 메서드 | 엔드포인트 | 설명 | 비고 | 제작여부 |
|------|--------|------------|------|------|----------|
| 채팅방 목록 조회 | GET | /api/chat/rooms | 내 채팅방 목록 | - | O |
| 채팅방 상세 조회 | GET | /api/chat/rooms/{roomId} | 채팅방 상세 | - | O |
| 메시지 목록 조회 | GET | /api/chat/rooms/{roomId}/messages | 메시지 목록 (커서 기반) | ?before=&size= | O |
| 메시지 전송 | POST | /api/chat/rooms/{roomId}/messages | 메시지 전송 | - | O |
| 메시지 읽음 처리 | PUT | /api/chat/rooms/{roomId}/messages/read | 메시지 읽음 처리 | - | O |

---

## 유저 추천 (Recommendation)

| 이름 | 메서드 | 엔드포인트 | 설명 | 비고 | 제작여부 |
|------|--------|------------|------|------|----------|
| 추천 목록 조회 | GET | /api/recommendations | 추천 유저 목록 | ?status= | O |
| 추천 수락 | POST | /api/recommendations/{recommendationId}/accept | 추천 수락 (채팅방 자동 생성) | - | O |
| 추천 거절 | POST | /api/recommendations/{recommendationId}/reject | 추천 거절 | - | O |

---

## 파일 (File)

| 이름 | 메서드 | 엔드포인트 | 설명 | 비고 | 제작여부 |
|------|--------|------------|------|------|----------|
| 파일 업로드 | POST | /api/files/upload | S3에 이미지 업로드 | multipart/form-data | O |
| 미션 인증 사진 업로드 | POST | /api/files/upload/mission-verify | mission_verify 폴더에 업로드 | multipart/form-data | O |
| 폴더 지정 업로드 | POST | /api/files/upload/{folder} | 지정 폴더에 업로드 | multipart/form-data | O |
| 파일 삭제 | DELETE | /api/files/{fileName} | S3에서 파일 삭제 | - | O |

---

## SSE 알림 (실시간)

| 이름 | 메서드 | 엔드포인트 | 설명 | 비고 | 제작여부 |
|------|--------|------------|------|------|----------|
| SSE 연결 | GET | /sse/connect | 실시간 알림 연결 | text/event-stream | O |
| 테스트 메시지 전송 | POST | /sse/send | 특정 사용자에게 메시지 전송 | - | O |
| 테스트 메시지 (GET) | GET | /sse/test | 본인에게 테스트 메시지 전송 | - | O |
| MISSION 알림 테스트 | GET | /sse/test/mission | 미션 알림 테스트 | - | O |
| DIARY 알림 테스트 | GET | /sse/test/diary | 일기 알림 테스트 | - | O |
| 커스텀 알림 테스트 | POST | /sse/test/custom | 커스텀 타입 알림 테스트 | - | O |

---

## 관리자 (Admin)

| 이름 | 메서드 | 엔드포인트 | 설명 | 비고 | 제작여부 |
|------|--------|------------|------|------|----------|
| 전체 회원 조회 | GET | /admin/members | 모든 회원 정보 조회 | ADMIN 권한 필요 | O |
| 특정 회원 조회 | GET | /admin/members/{memberId} | 특정 회원 정보 조회 | ADMIN 권한 필요 | O |
| 전체 카드 조회 | GET | /admin/card | 모든 카드 정보 조회 | ADMIN 권한 필요 | O |
| 알림 전송 (커스텀) | POST | /admin/send/custom | 특정 사용자에게 알림 전송 | ADMIN 권한 필요 | O |
| 일기 알림 전송 | POST | /admin/send/diary | 특정 사용자에게 일기 알림 전송 | ADMIN 권한 필요 | O |
| 리포트 알림 전송 | POST | /admin/send/report | 특정 사용자에게 리포트 알림 전송 | ADMIN 권한 필요 | O |
| 미션 데이터 초기화 | POST | /admin/reset-missions | 미션 관련 데이터 초기화 및 시드 | ADMIN 권한 필요 | O |
| 미션 수 확인 | GET | /admin/mission-count | 현재 미션 수 확인 | ADMIN 권한 필요 | O |
| 사용자 역할 변경 | PATCH | /admin/members/{memberId}/role | 사용자 역할 변경 | ADMIN 권한 필요 | O |
| 관리자 설정 | POST | /admin/setup-admin | 이메일로 관리자 설정 | 초기 설정용 | O |

---

## 관리자 미션 (AdminMission)

| 이름 | 메서드 | 엔드포인트 | 설명 | 비고 | 제작여부 |
|------|--------|------------|------|------|----------|
| 전체 미션 목록 조회 | GET | /api/admin/missions | 관리자용 전체 미션 조회 | ADMIN 권한 필요 | O |
| 미션 상세 조회 | GET | /api/admin/missions/{missionId} | 관리자용 미션 상세 | ADMIN 권한 필요 | O |
| 미션 생성 | POST | /api/admin/missions | 새 시스템 미션 생성 | ADMIN 권한 필요 | O |
| 미션 수정 | PUT | /api/admin/missions/{missionId} | 미션 수정 | ADMIN 권한 필요 | O |
| 미션 삭제 | DELETE | /api/admin/missions/{missionId} | 미션 삭제 | ADMIN 권한 필요 | O |
| 미션 활성화/비활성화 | PATCH | /api/admin/missions/{missionId}/toggle-active | 미션 활성화 토글 | ADMIN 권한 필요 | O |
| 미션 대량 등록 | POST | /api/admin/missions/bulk | JSON 배열로 여러 미션 등록 | ADMIN 권한 필요 | O |
