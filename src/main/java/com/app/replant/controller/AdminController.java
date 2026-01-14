package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.controller.dto.AdminDiaryNotificationRequestDto;
import com.app.replant.controller.dto.NotificationSendRequestDto;
import com.app.replant.controller.dto.UserResponseDto;
import com.app.replant.domain.notification.enums.NotificationType;
import com.app.replant.domain.notification.service.NotificationService;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
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
import java.util.stream.Collectors;

@Tag(name = "관리자", description = "관리자 전용 API (ADMIN 권한 필요)")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "JWT Token")
public class AdminController {

    private final SseService sseService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @Operation(summary = "전체 회원 조회", description = "모든 회원 정보를 조회합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAllMembers() {
        log.info("관리자 - 전체 회원 조회");
        List<User> users = userRepository.findAll();
        List<UserResponseDto> members = users.stream()
                .map(UserResponseDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.res(200, "사용자들을 정보를 불러왔습니다!", members));
    }

    @Operation(summary = "특정 회원 조회", description = "회원 ID로 특정 회원 정보를 조회합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @GetMapping("/members/{userId}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getMemberById(
            @Parameter(description = "조회할 회원 ID", required = true) @PathVariable Long userId) {
        log.info("관리자 - 회원 조회: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return ResponseEntity.ok(ApiResponse.res(200, "사용자 정보를 불러왔습니다!", UserResponseDto.from(user)));
    }


    @Operation(summary = "특정 사용자에게 알림 전송", description = "특정 사용자에게 SSE를 통해 알림을 전송합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 전송 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음 또는 SSE 연결 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @PostMapping("/send/custom")
    public ResponseEntity<ApiResponse<Void>> sendNotification(
            @Parameter(description = "알림 전송 요청 정보", required = true) @Valid @RequestBody NotificationSendRequestDto requestDto) {
        log.info("관리자 - 알림 전송 요청: email={}, message={}", requestDto.getMemberId(), requestDto.getMessage());

        // 이메일로 회원 찾기
        User user = userRepository.findByEmail(requestDto.getMemberId())
                .orElseThrow(() -> {
                    log.warn("관리자 - 알림 전송 실패: 회원을 찾을 수 없음 - email={}", requestDto.getMemberId());
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        Long userId = user.getId();
        log.info("관리자 - 회원 조회 성공: 이메일={}, DB ID={}, 닉네임={}",
                requestDto.getMemberId(), userId, user.getNickname());

        boolean sent = sseService.sendToUser(userId, "message", requestDto.getMessage());

        if (sent) {
            log.info("관리자 - 알림 전송 성공: DB ID={}, 이메일={}, 메시지={}",
                    userId, requestDto.getMemberId(), requestDto.getMessage());
            return ResponseEntity.ok(ApiResponse.res(200, "알림이 성공적으로 전송되었습니다."));
        } else {
            log.warn("관리자 - 알림 전송 실패: SSE 연결 없음 - DB ID={}, 이메일={}, 현재 연결된 사용자 수={}",
                    userId, requestDto.getMemberId(), sseService.getConnectedUserCount());
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(404,
                            String.format("해당 사용자(이메일: %s, DB ID: %d)가 SSE에 연결되어 있지 않습니다. 먼저 /sse/connect에 연결해주세요.",
                                    requestDto.getMemberId(), userId)));
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
        log.info("관리자 - 일기 알림 전송 요청: email={}", requestDto.getMemberId());

        // 이메일로 회원 찾기
        User user = userRepository.findByEmail(requestDto.getMemberId())
                .orElseThrow(() -> {
                    log.warn("관리자 - 일기 알림 전송 실패: 회원을 찾을 수 없음 - email={}", requestDto.getMemberId());
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        Long userId = user.getId();
        log.info("관리자 - 회원 조회 성공: 이메일={}, DB ID={}, 닉네임={}",
                requestDto.getMemberId(), userId, user.getNickname());

        // NotificationService를 통해 일기 알림 전송 (SSE + FCM)
        notificationService.createAndPushNotification(
                user,
                NotificationType.DIARY,
                "일기 알림",
                "오늘 하루는 어떠셨나요? 일기를 작성해보세요."
        );

        log.info("관리자 - 일기 알림 전송 완료: DB ID={}, 이메일={}", userId, requestDto.getMemberId());
        return ResponseEntity.ok(ApiResponse.res(200, "일기 알림이 성공적으로 전송되었습니다."));
    }

    @Operation(summary = "사용자 역할 변경", description = "특정 사용자의 역할을 변경합니다 (관리자 전용)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "역할 변경 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    @PatchMapping("/members/{userId}/role")
    public ResponseEntity<Map<String, Object>> updateMemberRole(
            @Parameter(description = "회원 ID", required = true) @PathVariable Long userId,
            @Parameter(description = "변경할 역할 (USER, GRADUATE, CONTRIBUTOR, ADMIN)", required = true) @RequestParam String role) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("관리자 - 회원 역할 변경: userId={}, role={}", userId, role);
            int updated = jdbcTemplate.update("UPDATE `user` SET role = ? WHERE id = ?", role, userId);
            if (updated > 0) {
                response.put("success", true);
                response.put("message", "역할이 변경되었습니다.");
                response.put("userId", userId);
                response.put("newRole", role);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "회원을 찾을 수 없습니다.");
                return ResponseEntity.status(404).body(response);
            }
        } catch (Exception e) {
            log.error("역할 변경 실패", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

}
