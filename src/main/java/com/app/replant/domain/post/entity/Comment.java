package com.app.replant.domain.post.entity;

import com.app.replant.common.SoftDeletableEntity;
import com.app.replant.domain.post.enums.CommentTargetType;
import com.app.replant.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comment", indexes = {
    @Index(name = "idx_comment_post_id", columnList = "post_id"),
    @Index(name = "idx_comment_parent_id", columnList = "parent_id"),
    @Index(name = "idx_comment_target", columnList = "target_type, target_id")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    // 일반화된 댓글 대상 (QnA, Diary 등에서 사용)
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private CommentTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 부모 댓글 (null이면 최상위 댓글)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    // 자식 댓글들 (대댓글)
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> replies = new ArrayList<>();

    @Builder
    public Comment(Post post, User user, String content, Comment parent,
                   CommentTargetType targetType, Long targetId) {
        this.post = post;
        this.user = user;
        this.content = content;
        this.parent = parent;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public boolean isReply() {
        return this.parent != null;
    }

    public Long getParentId() {
        return this.parent != null ? this.parent.getId() : null;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public boolean isAuthor(Long userId) {
        return this.user.getId().equals(userId);
    }
}
