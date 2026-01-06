package com.app.replant.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentRequest {

    @NotBlank(message = "댓글 내용은 필수입니다.")
    private String content;

    // 대댓글인 경우 부모 댓글 ID (최상위 댓글이면 null)
    private Long parentId;
}
