package com.app.replant.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "카드 추천 DTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardRecommendDto {

    @Schema(description = "카드 ID", example = "1")
    private Long cardId;

    @Schema(description = "카드 이름", example = "환경 지킴이 카드")
    private String cardName;

    @Schema(description = "카드 설명", example = "환경 보호 활동에 참여한 유저에게 주어지는 카드입니다.")
    private String description;

    @Schema(description = "카드 이미지 URL", example = "https://...")
    private String imageUrl;

    @Schema(description = "추천 이유", example = "최근 환경 미션을 많이 완료하셨네요!")
    private String recommendReason;
}
