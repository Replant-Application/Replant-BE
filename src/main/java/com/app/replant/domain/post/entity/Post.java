package com.app.replant.domain.post.entity;

import com.app.replant.global.common.BaseEntity;
import com.app.replant.domain.post.enums.PostType;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.usermission.entity.UserMission;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 게시글 엔티티 (통합)
 * - GENERAL: 일반 게시글 (자유 게시판)
 * - VERIFICATION: 인증 게시글 (미션 인증)
 *
 * 좋아요 = 인증 로직:
 * - VERIFICATION 타입의 좋아요 수가 REQUIRED_LIKES 이상이면 자동 인증 완료
 */
@Entity
@Table(name = "post", indexes = {
        @Index(name = "idx_post_type", columnList = "post_type"),
        @Index(name = "idx_post_user_id", columnList = "user_id"),
        @Index(name = "idx_post_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    // 인증에 필요한 좋아요 수 (설정값)
    public static final int REQUIRED_LIKES = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false, length = 20)
    private PostType postType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 인증글일 경우 UserMission 참조 (미션 정보는 여기서 가져옴)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_mission_id", unique = true)
    private UserMission userMission;

    // 게시글 제목
    // - GENERAL: 사용자가 입력한 제목
    // - VERIFICATION: 미션 제목 (성능 향상을 위해 저장)
    @Column(length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_urls", columnDefinition = "json")
    private String imageUrls;

    @Column(name = "del_flag", nullable = false)
    private Boolean delFlag = false;

    // 인증 상태 (VERIFICATION일 때만 사용): PENDING, APPROVED
    @Column(name = "status", length = 20)
    private String status;

    // 인증 완료 시간
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // 유효한 배지 보유 여부 (DB 컬럼 호환용)
    @Column(name = "has_valid_badge", nullable = false)
    private Boolean hasValidBadge = false;

    // 완료 정도 (VERIFICATION일 때만 사용, 0-100)
    @Column(name = "completion_rate")
    private Integer completionRate;

    // 댓글 관계
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    // 좋아요 관계
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostLike> likes = new ArrayList<>();

    // ========================================
    // 생성자 및 빌더
    // ========================================

    /**
     * 일반 게시글 생성
     */
    @Builder(builderMethodName = "generalBuilder")
    public Post(User user, String title, String content, String imageUrls) {
        this.postType = PostType.GENERAL;
        this.user = user;
        this.title = title;
        this.content = content;
        this.imageUrls = imageUrls;
        this.delFlag = false;
        this.hasValidBadge = false;
    }

    /**
     * 인증 게시글 생성
     */
    public static Post createVerificationPost(User user, UserMission userMission, String content, String imageUrls, Integer completionRate) {
        Post post = new Post();
        post.postType = PostType.VERIFICATION;
        post.user = user;
        post.userMission = userMission;
        // 인증글도 title 컬럼에 미션 제목 저장 (성능 향상 및 코드 단순화)
        post.title = userMission != null && userMission.getMission() != null
                ? userMission.getMission().getTitle()
                : "미션";
        post.content = content;
        post.imageUrls = imageUrls;
        post.status = "PENDING";
        post.delFlag = false;
        post.hasValidBadge = false;
        post.completionRate = completionRate;
        return post;
    }

    // ========================================
    // 업데이트 메서드
    // ========================================

    public void update(String title, String content, String imageUrls) {
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
        if (imageUrls != null) {
            this.imageUrls = imageUrls;
        }
    }

    public void updateVerificationContent(String content, String imageUrls, Integer completionRate) {
        if (this.postType != PostType.VERIFICATION) {
            throw new IllegalStateException("인증글만 이 메서드로 수정할 수 있습니다.");
        }
        if ("APPROVED".equals(this.status)) {
            throw new IllegalStateException("인증 완료 후에는 수정할 수 없습니다.");
        }
        this.content = content;
        this.imageUrls = imageUrls;
        if (completionRate != null) {
            this.completionRate = completionRate;
        }
    }

    // ========================================
    // 인증 관련 메서드 (좋아요 = 인증)
    // ========================================

    /**
     * 좋아요 수에 따른 인증 체크
     * 
     * @param likeCount 현재 좋아요 수
     * @return 인증 완료 여부 (이번에 새로 인증되었으면 true)
     */
    public boolean checkAndApproveByLikes(long likeCount) {
        if (this.postType != PostType.VERIFICATION) {
            return false;
        }
        if ("APPROVED".equals(this.status)) {
            return false; // 이미 인증됨
        }
        if (likeCount >= REQUIRED_LIKES) {
            this.status = "APPROVED";
            this.verifiedAt = LocalDateTime.now();
            return true; // 새로 인증됨
        }
        return false;
    }

    /**
     * 인증 완료 여부
     */
    public boolean isApproved() {
        return "APPROVED".equals(this.status);
    }

    /**
     * 인증 게시글을 직접 인증 처리 (좋아요 수와 관계없이 강제 인증)
     * 관리자용 또는 자동 처리용
     * 
     * @return 인증 처리 성공 여부 (이미 인증되었으면 false)
     */
    public boolean approve() {
        if (this.postType != PostType.VERIFICATION) {
            return false;
        }
        if ("APPROVED".equals(this.status)) {
            return false; // 이미 인증됨
        }
        this.status = "APPROVED";
        this.verifiedAt = LocalDateTime.now();
        return true; // 새로 인증됨
    }

    // ========================================
    // 유틸리티 메서드
    // ========================================

    public boolean isAuthor(Long userId) {
        return this.user.getId().equals(userId);
    }

    public void softDelete() {
        this.delFlag = true;
    }

    public void restore() {
        this.delFlag = false;
    }

    public boolean isDeleted() {
        return this.delFlag != null && this.delFlag;
    }

    public boolean isVerificationPost() {
        return this.postType == PostType.VERIFICATION;
    }

    public boolean isGeneralPost() {
        return this.postType == PostType.GENERAL;
    }

    /**
     * 미션 제목 반환 (인증글일 경우)
     */
    public String getMissionTitle() {
        if (this.userMission != null && this.userMission.getMission() != null) {
            return this.userMission.getMission().getTitle();
        }
        return null;
    }

    /**
     * 미션 ID 반환 (통합된 미션 ID)
     */
    public Long getMissionId() {
        if (this.userMission != null && this.userMission.getMission() != null) {
            return this.userMission.getMission().getId();
        }
        return null;
    }

    /**
     * 미션 타입 반환 (OFFICIAL / CUSTOM)
     */
    public String getMissionType() {
        if (this.userMission != null && this.userMission.getMission() != null) {
            return this.userMission.getMission().isOfficialMission() ? "OFFICIAL" : "CUSTOM";
        }
        return null;
    }

    /**
     * 식사 인증용 게시글로 변환 (userMission 없이)
     * MealLog와 연결되는 인증 게시글용
     */
    public void convertToMealVerification() {
        this.postType = PostType.VERIFICATION;
        this.status = "APPROVED";  // 식사 인증은 바로 승인
        this.verifiedAt = LocalDateTime.now();
    }
}
