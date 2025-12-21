package com.app.replant.controller.dto;

import com.app.replant.entity.type.CardType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CardRequestDto {
    private String name;
    private String description;
    private CardType cardType;
    private String imageUrl;
    private Integer pointsRequired;

    @Builder
    public CardRequestDto(String name, String description, CardType cardType,
                         String imageUrl, Integer pointsRequired) {
        this.name = name;
        this.description = description;
        this.cardType = cardType;
        this.imageUrl = imageUrl;
        this.pointsRequired = pointsRequired;
    }
}
