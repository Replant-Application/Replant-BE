package com.app.replant.domain.mission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MissionQnAAnswerRequest {

    @NotBlank(message = "답변 내용은 필수입니다")
    @Size(max = 1000, message = "답변은 1000자 이하여야 합니다")
    private String content;
}
