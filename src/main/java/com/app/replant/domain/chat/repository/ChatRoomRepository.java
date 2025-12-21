package com.app.replant.domain.chat.repository;

import com.app.replant.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT cr FROM ChatRoom cr WHERE (cr.user1.id = :userId OR cr.user2.id = :userId)")
    List<ChatRoom> findByUserId(@Param("userId") Long userId);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.id = :roomId AND (cr.user1.id = :userId OR cr.user2.id = :userId)")
    Optional<ChatRoom> findByIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
