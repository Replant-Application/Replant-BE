package com.app.replant.domain.chat.repository;

import com.app.replant.domain.chat.entity.ChatLog;
import com.app.replant.domain.chat.enums.ChatStatus;
import com.app.replant.domain.chat.enums.LLMProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {

    /**
     * 사용자별 채팅 이력 조회 (최신순, Reant 즉시 로딩)
     */
    @EntityGraph(attributePaths = {"reant"})
    Page<ChatLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 사용자별 최근 N개 채팅 조회
     */
    List<ChatLog> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 특정 기간 내 채팅 수 조회
     */
    @Query("SELECT COUNT(c) FROM ChatLog c WHERE c.createdAt BETWEEN :start AND :end")
    Long countByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * LLM Provider별 사용량 조회
     */
    @Query("SELECT c.llmProvider, COUNT(c) FROM ChatLog c WHERE c.createdAt BETWEEN :start AND :end GROUP BY c.llmProvider")
    List<Object[]> countByProviderBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 상태별 채팅 수 조회 (에러 모니터링용)
     */
    Long countByStatusAndCreatedAtAfter(ChatStatus status, LocalDateTime after);

    /**
     * 사용자별 오늘 채팅 수 조회 (Rate Limiting용)
     */
    @Query("SELECT COUNT(c) FROM ChatLog c WHERE c.user.id = :userId AND c.createdAt >= :todayStart")
    Long countTodayChatsByUserId(@Param("userId") Long userId, @Param("todayStart") LocalDateTime todayStart);

    /**
     * 사용자별 마지막 채팅 시간 조회
     */
    @Query("SELECT MAX(c.createdAt) FROM ChatLog c WHERE c.user.id = :userId")
    LocalDateTime findLastChatTimeByUserId(@Param("userId") Long userId);

    /**
     * 사용자별 오늘 선제 메시지 수 조회 (스팸 방지)
     */
    @Query("SELECT COUNT(c) FROM ChatLog c WHERE c.user.id = :userId AND c.isProactive = true AND c.createdAt >= :todayStart")
    Long countProactiveTodayByUserId(@Param("userId") Long userId, @Param("todayStart") LocalDateTime todayStart);

    /**
     * 채팅 기록이 있는 사용자 ID 목록 조회
     */
    @Query("SELECT DISTINCT c.user.id FROM ChatLog c")
    List<Long> findDistinctUserIds();
}
