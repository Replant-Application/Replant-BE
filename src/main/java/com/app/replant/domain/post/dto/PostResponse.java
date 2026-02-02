package com.app.replant.domain.post.dto;

import com.app.replant.domain.post.entity.Post;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 게시글 응답 DTO (단순화)
 */
@Getter
@Builder
public class PostResponse {

    private Long id;
    private String postType;  // GENERAL, VERIFICATION
    private Long userId;
    private String userNickname;
    private String userProfileImg;
    /** 작성자 리앤트 레벨 (커뮤니티에서 캐릭터 이미지 표시용, null이면 앱에서 1로 처리) */
    private Integer userReantLevel;

    // 미션 정보 (인증글일 경우)
    private MissionTag missionTag;

    private String title;
    private String content;
    private List<String> imageUrls;

    // 좋아요/댓글 수
    private Long likeCount;
    private Long commentCount;
    private Boolean isLiked;
    
    // 본인 게시글 여부 (프론트엔드에서 수정/삭제 버튼 표시용)
    private Boolean isAuthor;

    /** 공개 여부 (일반 게시글만 사용, 인증글은 항상 true) */
    private Boolean isPublic;

    // 인증 상태 (VERIFICATION일 경우)
    private String status;  // PENDING, APPROVED
    private LocalDateTime verifiedAt;
    private Integer completionRate; // 완료 정도 (0-100)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class MissionTag {
        private Long id;
        private String title;
        private String type; // OFFICIAL, CUSTOM
    }

    public static PostResponse from(Post post) {
        return from(post, 0L, 0L, false, null);
    }

    public static PostResponse from(Post post, Long commentCount) {
        return from(post, commentCount, 0L, false, null);
    }

    public static PostResponse from(Post post, Long commentCount, Long likeCount, Boolean isLiked) {
        return from(post, commentCount, likeCount, isLiked, null);
    }

    public static PostResponse from(Post post, Long commentCount, Long likeCount, Boolean isLiked, Long currentUserId) {
        // NPE 방어: User null 체크
        Long userId = null;
        String userNickname = null;
        String userProfileImg = null;
        Integer userReantLevel = null;
        if (post.getUser() != null) {
            var u = post.getUser();
            userId = u.getId();
            userNickname = u.getNickname();
            userProfileImg = u.getProfileImg();
            if (u.getReant() != null) {
                userReantLevel = u.getReant().getLevel();
            }
        }

        // 일반 게시글과 인증글 모두 title 컬럼 사용
        // 인증글의 경우 title이 비어있으면 미션 제목을 fallback으로 사용 (기존 데이터 호환성)
        String title = post.getTitle();
        String missionTitle = null;
        
        // 미션 정보 설정 (인증글일 경우 userMission -> mission 경로로 가져옴)
        if (post.getUserMission() != null && post.getUserMission().getMission() != null) {
            var mission = post.getUserMission().getMission();
            Long missionId = mission.getId();
            missionTitle = mission.getTitle();
            String missionType = mission.isOfficialMission() ? "OFFICIAL" : "CUSTOM";

            // 인증글인 경우: title이 비어있으면 무조건 미션 제목 사용
            if (post.isVerificationPost()) {
                if (title == null || title.trim().isEmpty()) {
                    title = missionTitle != null ? missionTitle : "";
                }
            }
        }
        
        // 최종 fallback: 그래도 비어있으면 빈 문자열
        if (title == null || title.trim().isEmpty()) {
            title = "";
        }
        
        // 본인 게시글 여부 확인 (user_id로 비교)
        Boolean isAuthor = currentUserId != null && userId != null && userId.equals(currentUserId);
        
        PostResponseBuilder builder = PostResponse.builder()
                .id(post.getId())
                .postType(post.getPostType() != null ? post.getPostType().name() : "GENERAL")
                .userId(userId)
                .userNickname(userNickname)
                .userProfileImg(userProfileImg)
                .userReantLevel(userReantLevel)
                .title(title)
                .content(post.getContent())
                .imageUrls(parseImageUrls(post.getImageUrls()))
                .likeCount(likeCount)
                .commentCount(commentCount)
                .isLiked(isLiked)
                .isAuthor(isAuthor)
                .isPublic(post.isVerificationPost() ? Boolean.TRUE : (post.getIsPublic() != null ? post.getIsPublic() : true))
                .status(post.getStatus())
                .verifiedAt(post.getVerifiedAt())
                .completionRate(post.getCompletionRate())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt());

        // missionTag 설정
        if (missionTitle != null && post.getUserMission() != null && post.getUserMission().getMission() != null) {
            var mission = post.getUserMission().getMission();
            Long missionId = mission.getId();
            String missionType = mission.isOfficialMission() ? "OFFICIAL" : "CUSTOM";

            if (missionId != null) {
                builder.missionTag(MissionTag.builder()
                        .id(missionId)
                        .title(missionTitle)
                        .type(missionType)
                        .build());
            }
        }

        return builder.build();
    }

    private static List<String> parseImageUrls(String imageUrlsJson) {
        if (imageUrlsJson == null || imageUrlsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(imageUrlsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
}
