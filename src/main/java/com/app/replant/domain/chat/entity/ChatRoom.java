package com.app.replant.domain.chat.entity;

import com.app.replant.domain.recommendation.entity.UserRecommendation;
import com.app.replant.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false, unique = true)
    private UserRecommendation recommendation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ChatRoom(UserRecommendation recommendation, User user1, User user2, Boolean isActive) {
        this.recommendation = recommendation;
        this.user1 = user1;
        this.user2 = user2;
        this.isActive = isActive != null ? isActive : true;
        this.createdAt = LocalDateTime.now();
    }

    public void updateLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public User getOtherUser(Long userId) {
        if (user1.getId().equals(userId)) {
            return user2;
        }
        return user1;
    }
}
