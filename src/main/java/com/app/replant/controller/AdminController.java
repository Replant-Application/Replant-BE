package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.controller.dto.AdminDiaryNotificationRequestDto;
import com.app.replant.controller.dto.AdminReportNotificationRequestDto;
import com.app.replant.controller.dto.CardResponseDto;
import com.app.replant.controller.dto.MemberResponseDto;
import com.app.replant.controller.dto.NotificationSendRequestDto;
import com.app.replant.entity.Member;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import com.app.replant.repository.member.MemberRepository;
import com.app.replant.service.member.MemberService;
import com.app.replant.service.card.CardService;
import com.app.replant.service.sse.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "관리자", description = "관리자 전용 API (ADMIN 권한 필요)")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "JWT Token")
public class AdminController {

    private final MemberService memberService;
    private final CardService cardService;
    private final SseService sseService;
    private final MemberRepository memberRepository;
    private final JdbcTemplate jdbcTemplate;

    @Operation(summary = "전체 회원 조회", description = "모든 회원 정보를 조회합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<MemberResponseDto>>> getAllMembers() {
        log.info("관리자 - 전체 회원 조회");
        List<MemberResponseDto> members = memberService.getAllMembers();
        return ResponseEntity.ok(ApiResponse.res(200, "사용자들을 정보를 불러왔습니다!", members));
    }

    @Operation(summary = "특정 회원 조회", description = "회원 ID로 특정 회원 정보를 조회합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @GetMapping("/members/{memberId}")
    public ResponseEntity<ApiResponse<MemberResponseDto>> getMemberById(
            @Parameter(description = "조회할 회원 ID", required = true) @PathVariable Long memberId) {
        log.info("관리자 - 회원 조회: {}", memberId);
        MemberResponseDto member = memberService.getMemberByIdForAdmin(memberId);
        return ResponseEntity.ok(ApiResponse.res(200, "사용자 정보를 불러왔습니다!", member));
    }

    @Operation(summary = "전체 카드 조회", description = "tbl_card에 등록된 모든 카드 정보를 조회합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @GetMapping("/card")
    public ResponseEntity<ApiResponse<List<CardResponseDto>>> getAllCards() {
        log.info("관리자 - 전체 카드 조회");
        List<CardResponseDto> cards = cardService.getAllCards();
        return ResponseEntity.ok(ApiResponse.res(200, "카드 정보를 불러왔습니다!", cards));
    }

    @Operation(summary = "특정 사용자에게 알림 전송", description = "특정 사용자에게 SSE를 통해 알림을 전송합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 전송 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음 또는 SSE 연결 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @PostMapping("/send/custom")
    public ResponseEntity<ApiResponse<Void>> sendNotification(
            @Parameter(description = "알림 전송 요청 정보", required = true) @Valid @RequestBody NotificationSendRequestDto requestDto) {
        log.info("관리자 - 알림 전송 요청: memberId(이메일)={}, message={}", requestDto.getMemberId(), requestDto.getMessage());

        // 이메일로 회원 찾기
        Member member = memberRepository.findByMemberId(requestDto.getMemberId())
                .orElseThrow(() -> {
                    log.warn("관리자 - 알림 전송 실패: 회원을 찾을 수 없음 - memberId(이메일)={}", requestDto.getMemberId());
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        Long memberId = member.getId();
        log.info("관리자 - 회원 조회 성공: 이메일={}, DB ID={}, 이름={}",
                requestDto.getMemberId(), memberId, member.getMemberName());

        boolean sent = sseService.sendToUser(memberId, "message", requestDto.getMessage());

        if (sent) {
            log.info("관리자 - 알림 전송 성공: DB ID={}, 이메일={}, 메시지={}",
                    memberId, requestDto.getMemberId(), requestDto.getMessage());
            return ResponseEntity.ok(ApiResponse.res(200, "알림이 성공적으로 전송되었습니다."));
        } else {
            log.warn("관리자 - 알림 전송 실패: SSE 연결 없음 - DB ID={}, 이메일={}, 현재 연결된 사용자 수={}",
                    memberId, requestDto.getMemberId(), sseService.getConnectedUserCount());
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(404,
                            String.format("해당 사용자(이메일: %s, DB ID: %d)가 SSE에 연결되어 있지 않습니다. 먼저 /sse/connect에 연결해주세요.",
                                    requestDto.getMemberId(), memberId)));
        }
    }

    @Operation(summary = "특정 사용자에게 일기 알림 전송", description = "특정 사용자에게 SSE를 통해 일기 알림을 전송합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 전송 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음 또는 SSE 연결 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @PostMapping("/send/diary")
    public ResponseEntity<ApiResponse<Void>> sendDiaryNotification(
            @Parameter(description = "일기 알림 전송 요청 정보", required = true) @Valid @RequestBody AdminDiaryNotificationRequestDto requestDto) {
        log.info("관리자 - 일기 알림 전송 요청: memberId(이메일)={}", requestDto.getMemberId());

        // 이메일로 회원 찾기
        Member member = memberRepository.findByMemberId(requestDto.getMemberId())
                .orElseThrow(() -> {
                    log.warn("관리자 - 일기 알림 전송 실패: 회원을 찾을 수 없음 - memberId(이메일)={}", requestDto.getMemberId());
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        Long memberId = member.getId();
        log.info("관리자 - 회원 조회 성공: 이메일={}, DB ID={}, 이름={}",
                requestDto.getMemberId(), memberId, member.getMemberName());

        // SSE를 통해 일기 알림 전송
        sseService.sendDiaryNotification(memberId);

        log.info("관리자 - 일기 알림 전송 완료: DB ID={}, 이메일={}", memberId, requestDto.getMemberId());
        return ResponseEntity.ok(ApiResponse.res(200, "일기 알림이 성공적으로 전송되었습니다."));
    }

    @Operation(summary = "특정 사용자에게 리포트 알림 전송", description = "특정 사용자에게 SSE를 통해 리포트 알림을 전송합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 전송 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음 또는 SSE 연결 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @PostMapping("/send/report")
    public ResponseEntity<ApiResponse<Void>> sendReportNotification(
            @Parameter(description = "리포트 알림 전송 요청 정보", required = true) @Valid @RequestBody AdminReportNotificationRequestDto requestDto) {
        log.info("관리자 - 리포트 알림 전송 요청: memberId(이메일)={}", requestDto.getMemberId());

        // 이메일로 회원 찾기
        Member member = memberRepository.findByMemberId(requestDto.getMemberId())
                .orElseThrow(() -> {
                    log.warn("관리자 - 리포트 알림 전송 실패: 회원을 찾을 수 없음 - memberId(이메일)={}", requestDto.getMemberId());
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        Long memberId = member.getId();
        log.info("관리자 - 회원 조회 성공: 이메일={}, DB ID={}, 이름={}",
                requestDto.getMemberId(), memberId, member.getMemberName());

        // 현재 월 가져오기
        int currentMonth = java.time.LocalDate.now().getMonthValue();

        // SSE를 통해 리포트 알림 전송 (현재 월 자동 사용)
        sseService.sendReportNotification(memberId, currentMonth);

        log.info("관리자 - 리포트 알림 전송 완료: DB ID={}, 이메일={}, month={}",
                memberId, requestDto.getMemberId(), currentMonth);
        return ResponseEntity.ok(ApiResponse.res(200, "리포트 알림이 성공적으로 전송되었습니다."));
    }

    @Operation(summary = "미션 데이터 초기화 및 시드", description = "유저 정보는 유지하고 미션 관련 데이터만 초기화합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "초기화 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "초기화 실패")
    @PostMapping("/reset-missions")
    public ResponseEntity<Map<String, Object>> resetMissions() {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("미션 데이터 초기화 시작...");

            // 외래키 체크 비활성화
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");

            // 미션 관련 테이블 초기화 (유저 데이터 유지)
            jdbcTemplate.execute("DELETE FROM chat_message");
            jdbcTemplate.execute("DELETE FROM chat_room");
            jdbcTemplate.execute("DELETE FROM user_recommendation");
            jdbcTemplate.execute("DELETE FROM notification");
            jdbcTemplate.execute("DELETE FROM comment");
            jdbcTemplate.execute("DELETE FROM post");
            jdbcTemplate.execute("DELETE FROM mission_qna_answer");
            jdbcTemplate.execute("DELETE FROM mission_qna");
            jdbcTemplate.execute("DELETE FROM mission_review");
            jdbcTemplate.execute("DELETE FROM verification_vote");
            jdbcTemplate.execute("DELETE FROM mission_verification");
            jdbcTemplate.execute("DELETE FROM verification_post");
            jdbcTemplate.execute("DELETE FROM user_badge");
            jdbcTemplate.execute("DELETE FROM user_mission");
            jdbcTemplate.execute("DELETE FROM custom_mission");
            jdbcTemplate.execute("DELETE FROM mission");

            // 외래키 체크 활성화
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");

            log.info("미션 데이터 삭제 완료, 새 미션 데이터 삽입 중...");

            // 미션 데이터 삽입
            String insertMissions = """
                INSERT INTO mission (id, title, description, type, verification_type, gps_latitude, gps_longitude, gps_radius_meters, required_minutes, exp_reward, badge_duration_days, is_active) VALUES
                (1, '30분 산책하기', '동네를 30분 동안 산책하며 마음을 정리해보세요.', 'DAILY', 'TIME', NULL, NULL, NULL, 30, 15, 3, TRUE),
                (2, '물 8잔 마시기', '하루 동안 물 8잔(2L)을 마시고 인증해주세요.', 'DAILY', 'COMMUNITY', NULL, NULL, NULL, NULL, 10, 3, TRUE),
                (3, '10분 명상하기', '조용한 곳에서 10분간 명상을 해보세요.', 'DAILY', 'TIME', NULL, NULL, NULL, 10, 10, 3, TRUE),
                (4, '영어 단어 10개 외우기', '오늘 새로운 영어 단어 10개를 외워보세요.', 'DAILY', 'COMMUNITY', NULL, NULL, NULL, NULL, 10, 3, TRUE),
                (5, '아침 식사하기', '건강한 아침 식사를 하고 사진으로 인증해주세요.', 'DAILY', 'COMMUNITY', NULL, NULL, NULL, NULL, 10, 3, TRUE),
                (6, '일기 쓰기', '오늘 하루를 돌아보며 일기를 작성해주세요.', 'DAILY', 'COMMUNITY', NULL, NULL, NULL, NULL, 10, 3, TRUE),
                (7, '스트레칭 10분', '10분간 스트레칭으로 몸을 풀어주세요.', 'DAILY', 'TIME', NULL, NULL, NULL, 10, 10, 3, TRUE),
                (8, '책 1권 읽기', '이번 주에 책 1권을 완독하고 감상을 공유해주세요.', 'WEEKLY', 'COMMUNITY', NULL, NULL, NULL, NULL, 50, 7, TRUE),
                (9, '헬스장 3회 방문', '이번 주에 헬스장을 3회 방문해주세요.', 'WEEKLY', 'GPS', 37.5665, 126.9780, 500, NULL, 50, 7, TRUE),
                (10, '새로운 요리 도전', '이번 주에 처음 만들어보는 요리에 도전해보세요.', 'WEEKLY', 'COMMUNITY', NULL, NULL, NULL, NULL, 40, 7, TRUE),
                (11, '친구와 통화하기', '오랫동안 연락 못했던 친구와 통화해보세요.', 'WEEKLY', 'COMMUNITY', NULL, NULL, NULL, NULL, 30, 7, TRUE),
                (12, '주 3회 운동하기', '이번 주에 3회 이상 운동을 해주세요. 각 30분 이상.', 'WEEKLY', 'TIME', NULL, NULL, NULL, 90, 50, 7, TRUE),
                (13, '새로운 취미 시작', '이번 달에 새로운 취미를 시작해보세요.', 'MONTHLY', 'COMMUNITY', NULL, NULL, NULL, NULL, 100, 21, TRUE),
                (14, '5만원 저축하기', '이번 달에 5만원을 저축하고 인증해주세요.', 'MONTHLY', 'COMMUNITY', NULL, NULL, NULL, NULL, 80, 21, TRUE),
                (15, '봉사활동 참여', '이번 달에 봉사활동에 참여해보세요.', 'MONTHLY', 'COMMUNITY', NULL, NULL, NULL, NULL, 120, 21, TRUE)
                """;

            jdbcTemplate.execute(insertMissions);

            // 미션 수 확인
            Integer missionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mission", Integer.class);

            log.info("미션 데이터 초기화 완료! 총 {} 개의 미션", missionCount);

            response.put("success", true);
            response.put("message", "미션 데이터가 초기화되었습니다.");
            response.put("missionCount", missionCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("미션 데이터 초기화 실패", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Operation(summary = "현재 미션 수 확인", description = "현재 DB에 저장된 미션 수를 확인합니다")
    @GetMapping("/mission-count")
    public ResponseEntity<Map<String, Object>> getMissionCount() {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer missionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mission", Integer.class);
            response.put("success", true);
            response.put("missionCount", missionCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
