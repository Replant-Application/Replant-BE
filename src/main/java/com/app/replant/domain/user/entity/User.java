package com.app.replant.domain.user.entity;

import com.app.replant.common.BaseEntity;
import com.app.replant.domain.mission.enums.PlaceType;
import com.app.replant.domain.mission.enums.WorryType;
import com.app.replant.domain.user.enums.Gender;
import com.app.replant.domain.user.enums.UserRole;
import com.app.replant.domain.user.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "`user`", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_nickname", columnList = "nickname"),
    @Index(name = "idx_user_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Email(message = "유효한 이메일 형식이어야 합니다")
    @NotBlank(message = "이메일은 필수입니다")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "닉네임은 필수입니다")
    @Size(min = 2, max = 20, message = "닉네임은 2-20자 사이여야 합니다")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9_-]+$", message = "닉네임은 한글, 영문, 숫자, _, -만 사용 가능합니다")
    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "password")
    private String password;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "profile_img", length = 500)
    private String profileImg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "total_missions_completed", nullable = false)
    private Integer totalMissionsCompleted = 0;

    @Column(name = "total_exp_gained", nullable = false)
    private Integer totalExpGained = 0;

    @Column(name = "login_streak", nullable = false)
    private Integer loginStreak = 0;

    @Column(name = "last_login_date")
    private LocalDate lastLoginDate;

    // ============ 사용자 맞춤 정보 필드들 ============

    // 고민 종류: RE_EMPLOYMENT(재취업), JOB_PREPARATION(취업준비), ENTRANCE_EXAM(입시),
    //          ADVANCEMENT(진학), RETURN_TO_SCHOOL(복학), RELATIONSHIP(연애)
    @Enumerated(EnumType.STRING)
    @Column(name = "worry_type", length = 20)
    private WorryType worryType;

    // 지역 (서울, 경기, 인천 등)
    @Column(name = "region", length = 50)
    private String region;

    // 선호 장소: HOME(집), OUTDOOR(야외), INDOOR(실내)
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_place_type", length = 10)
    private PlaceType preferredPlaceType;

    // FCM 토큰 (푸시 알림용)
    @Column(name = "fcm_token", length = 500)
    private String fcmToken;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserOauth> oauthList = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private com.app.replant.domain.reant.entity.Reant reant;

    @Builder
    private User(String email, String nickname, String password, String phone, LocalDate birthDate,
                 Gender gender, String profileImg, UserRole role, UserStatus status,
                 WorryType worryType, String region, PlaceType preferredPlaceType) {
        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.phone = phone;
        this.birthDate = birthDate;
        this.gender = gender;
        this.profileImg = profileImg;
        this.role = role != null ? role : UserRole.USER;
        this.status = status != null ? status : UserStatus.ACTIVE;
        // 사용자 맞춤 정보
        this.worryType = worryType;
        this.region = region;
        this.preferredPlaceType = preferredPlaceType;
    }

    public void updateProfile(String nickname, LocalDate birthDate, Gender gender, String profileImg,
                              WorryType worryType, String region, PlaceType preferredPlaceType) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (birthDate != null) {
            this.birthDate = birthDate;
        }
        if (gender != null) {
            this.gender = gender;
        }
        if (profileImg != null) {
            this.profileImg = profileImg;
        }
        // 사용자 맞춤 정보 업데이트
        if (worryType != null) {
            this.worryType = worryType;
        }
        if (region != null) {
            this.region = region;
        }
        if (preferredPlaceType != null) {
            this.preferredPlaceType = preferredPlaceType;
        }
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateLastLoginAt() {
        LocalDate today = LocalDate.now();
        this.lastLoginAt = LocalDateTime.now();

        // 연속 로그인 일수 계산
        if (this.lastLoginDate != null) {
            if (this.lastLoginDate.plusDays(1).equals(today)) {
                // 연속 로그인
                this.loginStreak++;
            } else if (!this.lastLoginDate.equals(today)) {
                // 연속 끊김
                this.loginStreak = 1;
            }
            // 같은 날이면 업데이트 안 함
        } else {
            this.loginStreak = 1;
        }

        this.lastLoginDate = today;
    }

    public void completeMission(int expGained) {
        this.totalMissionsCompleted++;
        this.totalExpGained += expGained;
    }

    public void promoteToGraduate() {
        this.role = UserRole.GRADUATE;
    }

    public void promoteToContributor() {
        this.role = UserRole.CONTRIBUTOR;
    }

    public void updateRole(UserRole role) {
        this.role = role;
    }

    public boolean isOAuthUser() {
        return password == null && !oauthList.isEmpty();
    }

    public boolean isEmailPasswordUser() {
        return password != null;
    }

    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }

    public boolean isGraduate() {
        return this.role == UserRole.GRADUATE;
    }

    public boolean isContributor() {
        return this.role == UserRole.CONTRIBUTOR;
    }

    public com.app.replant.domain.reant.entity.Reant getReant() {
        return this.reant;
    }

    /**
     * FCM 토큰 업데이트 (푸시 알림용)
     */
    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
