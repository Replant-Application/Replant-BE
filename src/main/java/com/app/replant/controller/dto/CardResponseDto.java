package com.app.replant.controller.dto;

import com.app.replant.entity.Card;
import com.app.replant.entity.type.CardType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "카드 응답 DTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardResponseDto {

    @Schema(description = "카드 ID", example = "1")
    private Long id;

    @Schema(description = "카드 이름", example = "환경 지킴이 카드")
    private String name;

    @Schema(description = "카드 설명", example = "환경 보호 활동에 참여한 유저에게 주어지는 카드입니다.")
    private String description;

    @Schema(description = "카드 타입", example = "SPECIAL")
    private CardType cardType;

    @Schema(description = "카드 이미지 URL", example = "https://...")
    private String imageUrl;

    @Schema(description = "획득 필요 포인트", example = "100")
    private Integer pointsRequired;

    public static CardResponseDto fromEntity(Card card) {
        return CardResponseDto.builder()
                .id(card.getId())
                .name(card.getName())
                .description(card.getDescription())
                .cardType(card.getCardType())
                .imageUrl(card.getImageUrl())
                .pointsRequired(card.getPointsRequired())
                .build();
    }
}
