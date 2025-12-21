package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.domain.notification.dto.NotificationResponse;
import com.app.replant.domain.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
        int readCount = notificationService.markAllNotificationsAsRead(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("readCount", readCount);
        result.put("message", readCount + "개의 알림을 읽음 처리했습니다.");

        return ApiResponse.success(result);
    }
}
