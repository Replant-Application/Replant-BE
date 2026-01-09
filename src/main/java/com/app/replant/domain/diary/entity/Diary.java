package com.app.replant.domain.diary.entity;

import com.app.replant.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "diary", indexes = {
    @Index(name = "idx_diary_user_date", columnList = "user_id, date")
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

    @Column
    private Integer mood; // 기분 값 (1-5 또는 슬라이더 값)

    @Column(name = "emotions", columnDefinition = "json")
    private String emotions; // 선택된 감정들 (JSON 배열: ["행복", "기쁨", "사랑"])

    @Column(name = "emotion_factors", columnDefinition = "json")
    private String emotionFactors; // 감정에 영향을 준 요인들 (JSON 배열: ["공부", "가족", "운동"])

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    public Diary(User user, LocalDate date, Integer mood, String emotions, String emotionFactors, String content) {
        this.user = user;
        this.date = date;
        this.mood = mood;
        this.emotions = emotions;
        this.emotionFactors = emotionFactors;
        this.content = content;
    }

    public void update(Integer mood, String emotions, String emotionFactors, String content) {
        if (mood != null) {
            this.mood = mood;
        }
        if (emotions != null) {
            this.emotions = emotions;
        }
        if (emotionFactors != null) {
            this.emotionFactors = emotionFactors;
        }
        this.content = content;
    }
}
