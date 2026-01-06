package com.app.replant.domain.verification.entity;

import com.app.replant.common.BaseEntity;
import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.verification.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_mission_id", nullable = false, unique = true)
    private UserMission userMission;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_urls", columnDefinition = "json")
    private String imageUrls;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus status;

    @Column(name = "approve_count", nullable = false)
    private Integer approveCount;

    @Column(name = "reject_count", nullable = false)
    private Integer rejectCount;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Builder
    public VerificationPost(User user, UserMission userMission, String content, String imageUrls, VerificationStatus status) {
        this.user = user;
        this.userMission = userMission;
        this.content = content;
        this.imageUrls = imageUrls;
        this.status = status != null ? status : VerificationStatus.PENDING;
        this.approveCount = 0;
        this.rejectCount = 0;
    }

    public void updateContent(String content, String imageUrls) {
        if (this.status != VerificationStatus.PENDING) {
            throw new IllegalStateException("인증 완료 후에는 수정할 수 없습니다.");
        }
        this.content = content;
        this.imageUrls = imageUrls;
    }

    // 승인/거절에 필요한 투표 수 (테스트용: 1, 운영용: 3)
    private static final int REQUIRED_APPROVE_COUNT = 1;
    private static final int REQUIRED_REJECT_COUNT = 3;

    public void addVote(boolean isApprove) {
        if (isApprove) {
            this.approveCount++;
            if (this.approveCount >= REQUIRED_APPROVE_COUNT) {
                this.status = VerificationStatus.APPROVED;
                this.verifiedAt = LocalDateTime.now();
            }
        } else {
            this.rejectCount++;
            if (this.rejectCount >= REQUIRED_REJECT_COUNT) {
                this.status = VerificationStatus.REJECTED;
            }
        }
    }
}
