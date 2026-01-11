package com.app.replant.domain.chat.repository;

/*
 * TODO: 채팅 기능 구현 시 주석 해제
 * 현재 채팅 기능은 미사용으로 Repository 비활성화 상태
 */

/*
import com.app.replant.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.room.id = :roomId " +
           "AND (:before IS NULL OR cm.id < :before) " +
           "ORDER BY cm.id DESC")
    List<ChatMessage> findByRoomIdWithCursor(@Param("roomId") Long roomId, @Param("before") Long before, Pageable pageable);

    @Modifying
    @Query("UPDATE ChatMessage cm SET cm.isRead = true WHERE cm.room.id = :roomId AND cm.sender.id != :userId AND cm.isRead = false")
    int markMessagesAsRead(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.room.id = :roomId AND cm.isRead = false AND cm.sender.id != :userId")
    long countUnreadMessages(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
*/
