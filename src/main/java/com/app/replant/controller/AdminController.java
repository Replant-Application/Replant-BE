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
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
