package com.app.replant.domain.diary.entity;

import com.app.replant.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "diary", indexes = {
    @Index(name = "idx_diary_user_date", columnList = "user_id, date"),
    @Index(name = "idx_diary_user_created", columnList = "user_id, created_at DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 50)
    private String emotion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 50)
    private String weather;

    @Column(length = 100)
    private String location;

    @Column(name = "image_urls", columnDefinition = "JSON")
    private String imageUrls;

    @Column(name = "is_private", nullable = false)
    private Boolean isPrivate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Diary(User user, LocalDate date, String emotion, String content, String weather, String location, String imageUrls, Boolean isPrivate) {
        this.user = user;
        this.date = date;
        this.emotion = emotion;
        this.content = content;
        this.weather = weather;
        this.location = location;
        this.imageUrls = imageUrls;
        this.isPrivate = isPrivate != null ? isPrivate : false;
        this.createdAt = LocalDateTime.now();
    }

    public void update(String emotion, String content, String weather, String location, String imageUrls, Boolean isPrivate) {
        this.emotion = emotion;
        this.content = content;
        this.weather = weather;
        this.location = location;
        this.imageUrls = imageUrls;
        if (isPrivate != null) {
            this.isPrivate = isPrivate;
        }
        this.updatedAt = LocalDateTime.now();
    }
}
