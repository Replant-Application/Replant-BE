package com.app.replant.controller;

import com.app.replant.common.ApiResponseWrapper;
import com.app.replant.common.SuccessCode;
import com.app.replant.domain.notification.dto.FcmSendDto;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import com.app.replant.service.fcm.FcmService;
import org.springframework.transaction.annotation.Transactional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * FCM 관리하는 Controller
 *
 * @author : lee
 * @fileName : FcmController
 * @since : 2/21/24
 */
@Tag(name = "FCM", description = "FCM 푸시 알림 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/fcm")
@RequiredArgsConstructor
public class FcmController {

    private final FcmService fcmService;
    private final UserRepository userRepository;

    @Operation(summary = "FCM 푸시 메시지 전송", description = "로그인한 사용자의 등록된 FCM 토큰으로 푸시 알림을 전송합니다. token 필드가 없으면 자동으로 등록된 토큰을 사용합니다.")
    @PostMapping("/send")
    public ResponseEntity<ApiResponseWrapper<Object>> pushMessage(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Validated FcmSendDto fcmSendDto) throws IOException {
        
        // token이 없으면 등록된 토큰을 자동으로 조회
        String token = fcmSendDto.getToken();
        if (token == null || token.isEmpty()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            
            if (user.getFcmToken() == null || user.getFcmToken().isEmpty()) {
                throw new CustomException(ErrorCode.NOT_FOUND, "FCM 토큰이 등록되지 않았습니다. 먼저 /api/notifications/fcm/token으로 토큰을 등록해주세요.");
            }
            
            token = user.getFcmToken();
            log.debug("[FCM] 등록된 토큰 자동 조회 - userId: {}", userId);
        }
        
        // 토큰을 포함한 DTO 생성
        FcmSendDto requestDto = FcmSendDto.builder()
                .token(token)
                .title(fcmSendDto.getTitle())
                .body(fcmSendDto.getBody())
                .build();
        
        log.debug("[+] 푸시 메시지를 전송합니다. userId: {}, title: {}", userId, fcmSendDto.getTitle());
        
        int result = fcmService.sendMessageTo(requestDto);
        
        // 전송 실패 시 유효하지 않은 토큰일 수 있으므로 User 테이블에서 제거
        if (result == 0) {
            log.warn("[FCM] 푸시 메시지 전송 실패 - userId: {}, 토큰이 유효하지 않을 수 있습니다", userId);
            // 유효하지 않은 토큰 제거 (다음에 다시 등록하도록)
            invalidateUserToken(userId);
        }

        ApiResponseWrapper<Object> arw = ApiResponseWrapper
                .builder()
                .result(result)
                .resultCode(result == 1 ? SuccessCode.SEND_SUCCESS.getStatus() : "400")
                .resultMsg(result == 1 ? SuccessCode.SEND_SUCCESS.getMessage() : "FCM 메시지 전송 실패. 토큰이 유효하지 않을 수 있습니다.")
                .build();
        return new ResponseEntity<>(arw, HttpStatus.OK);
    }
    
    /**
     * 사용자 FCM 토큰 무효화 (User 테이블에서 삭제)
     */
    @Transactional
    private void invalidateUserToken(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElse(null);
            if (user != null) {
                user.updateFcmToken(null);
                log.info("[FCM] 유효하지 않은 토큰 삭제 완료 - userId: {}", userId);
            }
        } catch (Exception e) {
            log.error("[FCM] 토큰 삭제 실패 - userId: {}", userId, e);
        }
    }
}