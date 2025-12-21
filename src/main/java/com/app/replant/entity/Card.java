package com.app.replant.entity;

import com.app.replant.entity.type.CardType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "card")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    private CardType cardType;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "points_required")
    private Integer pointsRequired;

    public String getCardImage() {
        return this.imageUrl;
    }

    public String getCardName() {
        return this.name;
    }

    public String getCardBenefit() {
        return this.description;
    }
}
