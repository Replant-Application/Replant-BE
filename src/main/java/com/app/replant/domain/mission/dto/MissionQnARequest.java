package com.app.replant.domain.mission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MissionQnARequest {

    @NotBlank(message = "질문 내용은 필수입니다")
    @Size(max = 500, message = "질문은 500자 이하여야 합니다")
    private String question;
}
