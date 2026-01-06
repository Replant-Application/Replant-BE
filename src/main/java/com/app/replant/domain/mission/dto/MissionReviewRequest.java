package com.app.replant.domain.mission.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MissionReviewRequest {

    @NotBlank(message = "리뷰 내용은 필수입니다")
    @Size(max = 1000, message = "리뷰는 1000자 이하여야 합니다")
    private String content;

    @NotNull(message = "별점은 필수입니다")
    @Min(value = 1, message = "별점은 1 이상이어야 합니다")
    @Max(value = 5, message = "별점은 5 이하여야 합니다")
    private Integer rating;
}
