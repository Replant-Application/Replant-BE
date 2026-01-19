package com.app.replant.domain.post.dto;

import com.app.replant.domain.post.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class CommentResponse {

    private Long id;
    private Long userId;
    private String userNickname;
    private String userProfileImg;
    private String content;
    private Long parentId;
    private List<CommentResponse> replies;
    private int replyCount;
    private Boolean isAuthor;  // 본인 댓글 여부 (프론트엔드에서 수정/삭제 버튼 표시용)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CommentResponse from(Comment comment) {
        return from(comment, null);
    }

    public static CommentResponse from(Comment comment, Long currentUserId) {
        // User가 이미 로딩되어 있어야 함 (LAZY 로딩 문제 방지)
        Long userId = comment.getUser() != null ? comment.getUser().getId() : null;
        String userNickname = comment.getUser() != null ? comment.getUser().getNickname() : "알 수 없음";
        String userProfileImg = comment.getUser() != null ? comment.getUser().getProfileImg() : null;

        // 본인 댓글 여부 확인 (user_id로 비교)
        Boolean isAuthor = currentUserId != null && userId != null && userId.equals(currentUserId);

        return CommentResponse.builder()
                .id(comment.getId())
                .userId(userId)
                .userNickname(userNickname)
                .userProfileImg(userProfileImg)
                .content(comment.getContent())
                .parentId(comment.getParentId())
                .replyCount(comment.getReplies() != null ? comment.getReplies().size() : 0)
                .isAuthor(isAuthor)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    /**
     * 대댓글까지 포함해서 반환 (최상위 댓글용)
     */
    public static CommentResponse fromWithReplies(Comment comment) {
        return fromWithReplies(comment, null);
    }

    public static CommentResponse fromWithReplies(Comment comment, Long currentUserId) {
        List<CommentResponse> replyResponses = null;
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            replyResponses = comment.getReplies().stream()
                    .map(reply -> CommentResponse.from(reply, currentUserId))
                    .collect(Collectors.toList());
        }

        // User가 이미 로딩되어 있어야 함 (LAZY 로딩 문제 방지)
        Long userId = comment.getUser() != null ? comment.getUser().getId() : null;
        String userNickname = comment.getUser() != null ? comment.getUser().getNickname() : "알 수 없음";
        String userProfileImg = comment.getUser() != null ? comment.getUser().getProfileImg() : null;

        // 본인 댓글 여부 확인 (user_id로 비교)
        Boolean isAuthor = currentUserId != null && userId != null && userId.equals(currentUserId);

        return CommentResponse.builder()
                .id(comment.getId())
                .userId(userId)
                .userNickname(userNickname)
                .userProfileImg(userProfileImg)
                .content(comment.getContent())
                .parentId(comment.getParentId())
                .replies(replyResponses)
                .replyCount(comment.getReplies() != null ? comment.getReplies().size() : 0)
                .isAuthor(isAuthor)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
