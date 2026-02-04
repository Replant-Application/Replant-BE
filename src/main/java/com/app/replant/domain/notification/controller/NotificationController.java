package com.app.replant.domain.notification.controller;

import com.app.replant.global.common.ApiResponse;
import com.app.replant.domain.notification.dto.FcmTokenRequest;
import com.app.replant.domain.notification.dto.NotificationResponse;
import com.app.replant.domain.notification.service.NotificationService;
import com.app.replant.global.exception.CustomException;
import com.app.replant.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 목록 조회")
    @GetMapping
    public ApiResponse<Map<String, Object>> getNotifications(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Boolean isRead,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (userId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        Page<NotificationResponse> notifications = notificationService.getNotifications(userId, isRead, pageable);
        long unreadCount = notificationService.getUnreadCount(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("content", notifications.getContent());
        result.put("totalElements", notifications.getTotalElements());
        result.put("totalPages", notifications.getTotalPages());
        result.put("number", notifications.getNumber());
        result.put("unreadCount", unreadCount);

        return ApiResponse.success(result);
    }

    @Operation(summary = "알림 읽음 처리")
    @PutMapping("/{notificationId}/read")
    public ApiResponse<Map<String, Object>> markNotificationAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal Long userId) {
        if (userId == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        notificationService.markNotificationAsRead(notificationId, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("id", notificationId);
        result.put("isRead", true);
        result.put("message", "알림을 읽음 처리했습니다.");

        return ApiResponse.success(result);
    }

    @Operation(summary = "전체 알림 읽음 처리")
    @PutMapping("/read-all")
    public ApiResponse<Map<String, Object>> markAllNotificationsAsRead(
            @AuthenticationPrincipal Long userId) {
        if (userId == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        int readCount = notificationService.markAllNotificationsAsRead(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("readCount", readCount);
        result.put("message", readCount + "개의 알림을 읽음 처리했습니다.");

        return ApiResponse.success(result);
    }

    @Operation(summary = "알림 삭제")
    @DeleteMapping("/{notificationId}")
    public ApiResponse<Map<String, String>> deleteNotification(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal Long userId) {
        if (userId == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        notificationService.deleteNotification(notificationId, userId);

        Map<String, String> result = new HashMap<>();
        result.put("message", "알림이 삭제되었습니다.");

        return ApiResponse.success(result);
    }

    @Operation(summary = "전체 알림 삭제 (해당 사용자의 모든 알림)")
    @DeleteMapping("/all")
    public ApiResponse<Map<String, Object>> deleteAllNotifications(
            @AuthenticationPrincipal Long userId) {
        if (userId == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        int deletedCount = notificationService.deleteAllNotifications(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", deletedCount);
        result.put("message", deletedCount + "개의 알림이 삭제되었습니다.");

        return ApiResponse.success(result);
    }

    @Operation(summary = "FCM 토큰 등록/업데이트", description = "사용자의 FCM 토큰을 등록하거나 업데이트합니다.")
    @PostMapping("/fcm/token")
    public ApiResponse<Map<String, String>> registerFcmToken(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FcmTokenRequest request) {
        if (userId == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        notificationService.registerFcmToken(userId, request.getFcmToken());

        Map<String, String> result = new HashMap<>();
        result.put("message", "FCM 토큰이 등록되었습니다.");

        return ApiResponse.success(result);
    }
}
