package com.app.replant.domain.reant.entity;

import com.app.replant.common.BaseEntity;
import com.app.replant.domain.reant.enums.ReantStage;
import com.app.replant.domain.user.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "reant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false)
    private Integer exp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReantStage stage;

    @Column(name = "max_level", nullable = false)
    private Integer maxLevel = 100;

    @Column(nullable = false)
    private Integer mood = 100; // 기분 (0-100)

    @Column(nullable = false)
    private Integer health = 100; // 건강도 (0-100)

    @Column(name = "hunger", nullable = false)
    private Integer hunger = 0; // 배고픔 (0-100, 높을수록 배고픔)

    @Column(columnDefinition = "json")
    private String appearance;

    @Builder
    private Reant(User user, String name, Integer level, Integer exp, ReantStage stage, String appearance) {
        this.user = user;
        this.name = name != null ? name : "리앤트";
        this.level = level != null ? level : 1;
        this.exp = exp != null ? exp : 0;
        this.stage = stage != null ? stage : ReantStage.EGG;
        this.appearance = appearance;
    }

    public void updateProfile(String name, String appearance) {
        if (name != null) {
            this.name = name;
        }
        if (appearance != null) {
            this.appearance = appearance;
        }
    }

    public Map<String, Object> getAppearanceAsMap() {
        if (appearance == null) {
            return new HashMap<>();
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(appearance, Map.class);
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    public void addExp(int expAmount) {
        this.exp += expAmount;

        // Mood 상승 (경험치 획득 시 기분 좋아짐)
        this.mood = Math.min(100, this.mood + 5);

        checkLevelUp();
    }

    public void feed() {
        this.hunger = Math.max(0, this.hunger - 30);
        this.health = Math.min(100, this.health + 5);
        this.mood = Math.min(100, this.mood + 10);
    }

    public void rest() {
        this.health = Math.min(100, this.health + 20);
        this.mood = Math.min(100, this.mood + 10);
    }

    public void play() {
        this.mood = Math.min(100, this.mood + 20);
        this.hunger = Math.min(100, this.hunger + 5);
    }

    public void pet() {
        this.mood = Math.min(100, this.mood + 15);
    }

    public void decreaseHunger() {
        this.hunger = Math.min(100, this.hunger + 10);
        if (this.hunger > 80) {
            this.mood = Math.max(0, this.mood - 10);
            this.health = Math.max(0, this.health - 5);
        }
    }

    private void checkLevelUp() {
        int nextLevelExp = calculateNextLevelExp();
        while (this.exp >= nextLevelExp && this.level < this.maxLevel) {
            this.level++;
            this.exp -= nextLevelExp;
            updateStage();
            // 레벨업 시 기분 최대치
            this.mood = 100;
            nextLevelExp = calculateNextLevelExp();
        }
    }

    private void updateStage() {
        if (this.level >= 30) {
            this.stage = ReantStage.ADULT;
        } else if (this.level >= 15) {
            this.stage = ReantStage.TEEN;
        } else if (this.level >= 5) {
            this.stage = ReantStage.BABY;
        } else {
            this.stage = ReantStage.EGG;
        }
    }

    private int calculateNextLevelExp() {
        return this.level * 100;
    }
}
