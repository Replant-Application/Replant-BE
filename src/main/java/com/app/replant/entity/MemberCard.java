package com.app.replant.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_card")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MemberCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Column(name = "acquired_at", nullable = false)
    private LocalDateTime acquiredAt;

    @Column(name = "is_new")
    private Boolean isNew;

    @PrePersist
    protected void onCreate() {
        acquiredAt = LocalDateTime.now();
        if (isNew == null) {
            isNew = true;
        }
    }

    public String getCardNum() {
        return this.id != null ? String.valueOf(this.id) : null;
    }

    public String getCardAlias() {
        return this.card != null ? this.card.getName() : null;
    }
}
