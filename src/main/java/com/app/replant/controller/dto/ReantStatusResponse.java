package com.app.replant.controller.dto;

import com.app.replant.domain.reant.entity.Reant;
import com.app.replant.domain.reant.enums.ReantStage;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReantStatusResponse {
    private Long id;
    private String name;
    private Integer level;
    private Integer exp;
    private ReantStage stage;
    private Integer mood;
    private Integer health;
    private Integer hunger;
    private Integer maxLevel;
    private Integer nextLevelExp;
    private String appearance;

    public static ReantStatusResponse from(Reant reant) {
        return ReantStatusResponse.builder()
                .id(reant.getId())
                .name(reant.getName())
                .level(reant.getLevel())
                .exp(reant.getExp())
                .stage(reant.getStage())
                .mood(reant.getMood())
                .health(reant.getHealth())
                .hunger(reant.getHunger())
                .maxLevel(reant.getMaxLevel())
                .nextLevelExp(reant.getLevel() * 100) // 다음 레벨 필요 경험치
                .appearance(reant.getAppearance())
                .build();
    }
}
