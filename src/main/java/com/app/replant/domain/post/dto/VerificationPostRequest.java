package com.app.replant.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 인증 게시글 작성 요청 DTO
 */
@Getter
@NoArgsConstructor
public class VerificationPostRequest {

    @NotNull(message = "미션 ID는 필수입니다.")
    private Long userMissionId;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    private List<String> imageUrls;
}
