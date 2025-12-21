package com.app.replant.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class PostRequest {

    private Long missionId;
    private Long customMissionId;

    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    private List<String> imageUrls;
}
