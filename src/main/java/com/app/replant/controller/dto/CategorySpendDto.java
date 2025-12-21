package com.app.replant.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "카테고리별 지출 DTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorySpendDto {

    @Schema(description = "카테고리 이름", example = "식비")
    private String categoryName;

    @Schema(description = "지출 금액", example = "150000")
    private Long amount;

    @Schema(description = "전체 지출 대비 비율 (%)", example = "35.5")
    private Double percentage;
}
