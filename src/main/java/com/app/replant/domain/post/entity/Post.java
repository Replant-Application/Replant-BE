package com.app.replant.domain.post.entity;

import com.app.replant.common.BaseEntity;
import com.app.replant.domain.custommission.entity.CustomMission;
import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_mission_id")
    private CustomMission customMission;

    @Column(length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_urls", columnDefinition = "json")
    private String imageUrls;

    @Column(name = "has_valid_badge", nullable = false)
    private Boolean hasValidBadge;

    @Builder
    public Post(User user, Mission mission, CustomMission customMission, String title, String content, String imageUrls, Boolean hasValidBadge) {
        this.user = user;
        this.mission = mission;
        this.customMission = customMission;
        this.title = title;
        this.content = content;
        this.imageUrls = imageUrls;
        this.hasValidBadge = hasValidBadge != null ? hasValidBadge : false;
    }

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

    public boolean isAuthor(Long userId) {
        return this.user.getId().equals(userId);
    }
}
