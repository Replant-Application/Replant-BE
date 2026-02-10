package com.app.replant.domain.reant.entity;

import com.app.replant.global.common.BaseEntity;
import com.app.replant.domain.reant.enums.ReantStage;
import com.app.replant.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})  // Hibernate 프록시 직렬화 방지
public class Reant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore  // 순환 참조 방지
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

    /**
     * 경험치 차감 (인증 게시글 삭제 시 사용)
     * 레벨 다운 처리 포함
     */
    public void subtractExp(int expAmount) {
        if (expAmount <= 0) {
            return;
        }

        // 경험치 차감 (0 이하로 떨어지지 않도록)
        this.exp = Math.max(0, this.exp - expAmount);

        // Mood 감소 (경험치 회수 시 기분 나빠짐)
        this.mood = Math.max(0, this.mood - 5);

        // 레벨 다운 체크
        checkLevelDown();
    }

    private void checkLevelDown() {
        // 현재 레벨에 필요한 경험치 계산
        int currentLevelExp = calculateCurrentLevelExp();
        
        // 경험치가 현재 레벨에 필요한 경험치보다 적으면 레벨 다운
        while (this.exp < currentLevelExp && this.level > 1) {
            this.level--;
            currentLevelExp = calculateCurrentLevelExp();
            updateStage();
        }
    }

    private int calculateCurrentLevelExp() {
        // 현재 레벨에 도달하기 위해 필요한 총 경험치 (레벨별 필요 경험치 합)
        int totalExp = 0;
        for (int i = 1; i < this.level; i++) {
            totalExp += getExpForNextLevel(i);
        }
        return totalExp;
    }

    /**
     * 해당 레벨에서 다음 레벨로 올라가는데 필요한 경험치
     * L1→2: 10, L2→3: 50, L3→4: 100, L4→5: 200, L5→6: 500, L6+: 500
     */
    public static int getExpForNextLevel(int level) {
        return switch (level) {
            case 1 -> 10;
            case 2 -> 50;
            case 3 -> 100;
            case 4 -> 200;
            case 5 -> 500;
            default -> 500;
        };
    }

    /** DTO/외부용: 현재 레벨에서 다음 레벨까지 필요한 경험치 */
    public int getNextLevelExp() {
        return getExpForNextLevel(this.level);
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
        return getExpForNextLevel(this.level);
    }
}
