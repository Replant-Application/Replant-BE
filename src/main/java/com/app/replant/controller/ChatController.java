package com.app.replant.controller;

/*
 * TODO: 채팅 기능 구현 시 주석 해제
 * 현재 채팅 기능은 미사용으로 Controller 비활성화 상태
 */

/*
import com.app.replant.common.ApiResponse;
import com.app.replant.domain.chat.dto.ChatMessageRequest;
import com.app.replant.domain.chat.dto.ChatMessageResponse;
import com.app.replant.domain.chat.dto.ChatRoomResponse;
import com.app.replant.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Chat", description = "채팅 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "채팅방 목록 조회")
    @GetMapping("/rooms")
    public ApiResponse<List<ChatRoomResponse>> getChatRooms(
            @AuthenticationPrincipal Long userId) {
        List<ChatRoomResponse> rooms = chatService.getChatRooms(userId);
        return ApiResponse.success(rooms);
    }

    @Operation(summary = "채팅방 상세 조회")
    @GetMapping("/rooms/{roomId}")
    public ApiResponse<ChatRoomResponse> getChatRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId) {
        ChatRoomResponse room = chatService.getChatRoom(roomId, userId);
        return ApiResponse.success(room);
    }

    @Operation(summary = "메시지 목록 조회 (커서 기반)")
    @GetMapping("/rooms/{roomId}/messages")
    public ApiResponse<List<ChatMessageResponse>> getChatMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        List<ChatMessageResponse> messages = chatService.getChatMessages(roomId, userId, before, size);
        return ApiResponse.success(messages);
    }

    @Operation(summary = "메시지 전송")
    @PostMapping("/rooms/{roomId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChatMessageResponse> sendMessage(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid ChatMessageRequest request) {
        ChatMessageResponse message = chatService.sendMessage(roomId, userId, request);
        return ApiResponse.success(message);
    }

    @Operation(summary = "메시지 읽음 처리")
    @PutMapping("/rooms/{roomId}/messages/read")
    public ApiResponse<Map<String, Object>> markMessagesAsRead(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId) {
        int readCount = chatService.markMessagesAsRead(roomId, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("roomId", roomId);
        result.put("readCount", readCount);
        result.put("message", readCount + "개의 메시지를 읽음 처리했습니다.");

        return ApiResponse.success(result);
    }
}
*/
