package com.app.replant.domain.chat.service;

import com.app.replant.domain.chat.dto.ChatMessageRequest;
import com.app.replant.domain.chat.dto.ChatMessageResponse;
import com.app.replant.domain.chat.dto.ChatRoomResponse;
import com.app.replant.domain.chat.entity.ChatMessage;
import com.app.replant.domain.chat.entity.ChatRoom;
import com.app.replant.domain.chat.repository.ChatMessageRepository;
import com.app.replant.domain.chat.repository.ChatRoomRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public List<ChatRoomResponse> getChatRooms(Long userId) {
        return chatRoomRepository.findByUserId(userId)
                .stream()
                .map(room -> {
                    long unreadCount = chatMessageRepository.countUnreadMessages(room.getId(), userId);
                    // Get last message if available
                    List<ChatMessage> lastMessages = chatMessageRepository.findByRoomIdWithCursor(room.getId(), null, PageRequest.of(0, 1));
                    if (!lastMessages.isEmpty()) {
                        ChatMessage lastMessage = lastMessages.get(0);
                        return ChatRoomResponse.from(room, userId, unreadCount, lastMessage.getContent(), lastMessage.getCreatedAt(), lastMessage.getIsRead());
                    }
                    return ChatRoomResponse.from(room, userId, unreadCount);
                })
                .collect(Collectors.toList());
    }

    public ChatRoomResponse getChatRoom(Long roomId, Long userId) {
        ChatRoom room = findChatRoomByIdAndUserId(roomId, userId);
        long unreadCount = chatMessageRepository.countUnreadMessages(roomId, userId);
        return ChatRoomResponse.from(room, userId, unreadCount);
    }

    public List<ChatMessageResponse> getChatMessages(Long roomId, Long userId, Long before, Integer size) {
        // Verify user has access to this room
        findChatRoomByIdAndUserId(roomId, userId);

        Pageable pageable = PageRequest.of(0, size != null ? size : 20);
        return chatMessageRepository.findByRoomIdWithCursor(roomId, before, pageable)
                .stream()
                .map(message -> ChatMessageResponse.from(message, userId))
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long roomId, Long userId, ChatMessageRequest request) {
        ChatRoom room = findChatRoomByIdAndUserId(roomId, userId);

        if (!room.getIsActive()) {
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_ACTIVE);
        }

        User sender = findUserById(userId);

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(request.getContent())
                .build();

        ChatMessage saved = chatMessageRepository.save(message);

        // Update room's last message time
        room.updateLastMessageAt(LocalDateTime.now());

        return ChatMessageResponse.from(saved, userId);
    }

    @Transactional
    public int markMessagesAsRead(Long roomId, Long userId) {
        // Verify user has access to this room
        findChatRoomByIdAndUserId(roomId, userId);

        return chatMessageRepository.markMessagesAsRead(roomId, userId);
    }

    private ChatRoom findChatRoomByIdAndUserId(Long roomId, Long userId) {
        return chatRoomRepository.findByIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
