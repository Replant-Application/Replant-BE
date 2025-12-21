package com.app.replant.controller.dto;

import com.app.replant.domain.reant.entity.Reant;
import com.app.replant.domain.reant.enums.ReantStage;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class ReantResponse {
    private Long id;
    private String name;
    private Integer level;
    private Integer exp;
    private ReantStage stage;
    private Map<String, Object> appearance;
    private Integer nextLevelExp;

    public static ReantResponse from(Reant reant) {
        return ReantResponse.builder()
                .id(reant.getId())
                .name(reant.getName())
                .level(reant.getLevel())
                .exp(reant.getExp())
                .stage(reant.getStage())
                .appearance(reant.getAppearanceAsMap())
                .nextLevelExp(reant.getLevel() * 100)
                .build();
    }
}
