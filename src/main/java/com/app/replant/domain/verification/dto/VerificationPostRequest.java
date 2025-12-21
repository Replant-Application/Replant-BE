package com.app.replant.domain.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class VerificationPostRequest {

    @NotNull(message = "유저 미션 ID는 필수입니다.")
    private Long userMissionId;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    private List<String> imageUrls;
}
