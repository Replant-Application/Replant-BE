package com.app.replant.domain.missionset.entity;

import com.app.replant.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 투두리스트 리뷰 엔티티
 * (구 MissionSetReview)
 */
@Entity
@Table(name = "todolist_review", uniqueConstraints = {
        @UniqueConstraint(name = "uk_todolist_user", columnNames = { "todolist_id", "user_id" })
}, indexes = {
        @Index(name = "idx_todolist_review_todolist", columnList = "todolist_id"),
        @Index(name = "idx_todolist_review_user", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodoListReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todolist_id", nullable = false)
    private TodoList todoList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer rating; // 1-5 별점

    @Column(columnDefinition = "TEXT")
    private String content; // 리뷰 내용 (선택)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    private TodoListReview(TodoList todoList, User user, Integer rating, String content) {
        this.todoList = todoList;
        this.user = user;
        this.rating = rating != null ? Math.min(5, Math.max(1, rating)) : 5;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(Integer rating, String content) {
        if (rating != null) {
            this.rating = Math.min(5, Math.max(1, rating));
        }
        if (content != null) {
            this.content = content;
        }
        this.updatedAt = LocalDateTime.now();
    }
}
