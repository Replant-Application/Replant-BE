package com.app.replant.domain.verification.entity;

import com.app.replant.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_vote", uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_voter", columnNames = {"verification_post_id", "voter_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_post_id", nullable = false)
    private VerificationPost verificationPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id", nullable = false)
    private User voter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VoteType vote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public VerificationVote(VerificationPost verificationPost, User voter, VoteType vote) {
        this.verificationPost = verificationPost;
        this.voter = voter;
        this.vote = vote;
        this.createdAt = LocalDateTime.now();
    }

    public enum VoteType {
        APPROVE, REJECT
    }
}
