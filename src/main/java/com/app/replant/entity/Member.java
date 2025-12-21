package com.app.replant.entity;

import com.app.replant.entity.type.Authority;
import com.app.replant.entity.type.StatusType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * @deprecated 이 엔티티는 더 이상 사용되지 않습니다.
 * {@link com.app.replant.domain.user.entity.User}를 사용하세요.
 * Member는 레거시 이메일/비밀번호 로그인 전용이며, User로 통합되었습니다.
 */
@Deprecated(since = "2.0", forRemoval = true)
@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, unique = true)
    private String memberId;

    @Column(nullable = false)
    private String password;

    @Column(name = "member_name", nullable = false)
    private String memberName;

    @Column(nullable = false)
    private String phone;

    @Column(name = "birth_date")
    private String birthDate;

    @Column(name = "birth_back")
    private String birthBack;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusType status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Authority authority;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
