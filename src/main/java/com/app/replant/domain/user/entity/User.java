package com.app.replant.domain.user.entity;

import com.app.replant.common.BaseEntity;
import com.app.replant.domain.mission.enums.PlaceType;
import com.app.replant.domain.mission.enums.WorryType;
import com.app.replant.domain.user.enums.Gender;
import com.app.replant.domain.user.enums.MetropolitanArea;
import com.app.replant.domain.user.enums.UserRole;
import com.app.replant.domain.user.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    // 지역 (광역자치단체)
    @Enumerated(EnumType.STRING)
    @Column(name = "region", length = 20)
    private MetropolitanArea region;

    // 선호 장소: HOME(집), OUTDOOR(야외), INDOOR(실내)
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_place_type", length = 10)
    private PlaceType preferredPlaceType;

    // FCM 토큰 (푸시 알림용)
    @Column(name = "fcm_token", length = 500)
    private String fcmToken;

    // 삭제 플래그 (소프트 삭제)
    @Column(name = "del_flag", nullable = false)
    private Boolean delFlag = false;

    // 삭제된 시간 (30일 후 완전 삭제용)
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 차단 여부
    @Column(name = "blocked", nullable = false)
    private Boolean blocked = false;

    // ============ 돌발 미션 설정 관련 필드 ============
    
    // 돌발 미션 설정 완료 여부
    @Column(name = "is_spontaneous_mission_setup_completed", nullable = false)
    private Boolean isSpontaneousMissionSetupCompleted = false;
    
    // 취침 시간 (HH:mm 형식, 예: "23:00")
    @Column(name = "sleep_time", length = 5)
    private String sleepTime;
    
    // 기상 시간 (HH:mm 형식, 예: "07:00")
    @Column(name = "wake_time", length = 5)
    private String wakeTime;
    
    // 식사 시간 (HH:mm 형식, null 가능 - 해당 식사를 안 먹는 경우)
    @Column(name = "breakfast_time", length = 5)
    private String breakfastTime;
    
    @Column(name = "lunch_time", length = 5)
    private String lunchTime;
    
    @Column(name = "dinner_time", length = 5)
    private String dinnerTime;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserOauth> oauthList = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore  // 순환 참조 방지 및 lazy loading 방지
    private com.app.replant.domain.reant.entity.Reant reant;

    @Builder
    private User(String email, String nickname, String password, String phone, LocalDate birthDate,
                 Gender gender, String profileImg, UserRole role, UserStatus status,
                 WorryType worryType, MetropolitanArea region, PlaceType preferredPlaceType) {
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
                              WorryType worryType, MetropolitanArea region, PlaceType preferredPlaceType) {
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

    @JsonIgnore  // Jackson 직렬화 시 lazy loading 방지
    public com.app.replant.domain.reant.entity.Reant getReant() {
        return this.reant;
    }

    /**
     * FCM 토큰 업데이트 (푸시 알림용)
     */
    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    /**
     * 돌발 미션 설정 완료 처리
     */
    public void setupSpontaneousMission(String sleepTime, String wakeTime, 
                                        String breakfastTime, String lunchTime, String dinnerTime) {
        this.isSpontaneousMissionSetupCompleted = true;
        this.sleepTime = sleepTime;
        this.wakeTime = wakeTime;
        this.breakfastTime = breakfastTime;
        this.lunchTime = lunchTime;
        this.dinnerTime = dinnerTime;
    }

    /**
     * 돌발 미션 설정 완료 여부 확인
     */
    public boolean isSpontaneousMissionSetupCompleted() {
        return Boolean.TRUE.equals(this.isSpontaneousMissionSetupCompleted);
    }

    /**
     * 회원 탈퇴 처리 (Soft Delete - 1단계)
     * - delFlag를 true로 설정
     * - status를 INACTIVE로 변경 (30일 후 DELETED로 변경됨)
     * - deletedAt을 현재 시간으로 설정 (30일 후 완전 삭제용)
     * - FCM 토큰 제거
     * - 개인정보 마스킹 (이메일, 닉네임 등)
     */
    public void softDelete() {
        this.delFlag = true;
        this.status = UserStatus.INACTIVE; // 탈퇴 시 INACTIVE 상태로 변경
        this.deletedAt = LocalDateTime.now(); // 30일 후 완전 삭제를 위한 시간 기록
        this.fcmToken = null; // FCM 토큰 제거
        
        // 개인정보 마스킹 (GDPR 준수)
        // 이메일은 유지 (재가입 방지용)하되, 닉네임은 변경
        if (this.nickname != null && !this.nickname.startsWith("탈퇴한사용자")) {
            this.nickname = "탈퇴한사용자" + this.id;
        }
        
        // 프로필 이미지 제거
        this.profileImg = null;
        
        // 돌발 미션 설정 초기화
        this.isSpontaneousMissionSetupCompleted = false;
        this.sleepTime = null;
        this.wakeTime = null;
        this.breakfastTime = null;
        this.lunchTime = null;
        this.dinnerTime = null;
    }

    /**
     * INACTIVE 상태를 DELETED로 변경 (30일 후 스케줄러에서 호출)
     */
    public void markAsDeleted() {
        this.status = UserStatus.DELETED;
    }

    /**
     * 삭제 여부 확인
     * INACTIVE 또는 DELETED 상태이거나 delFlag가 true인 경우 삭제된 것으로 간주
     */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(this.delFlag) 
                || this.status == UserStatus.INACTIVE 
                || this.status == UserStatus.DELETED;
    }

    /**
     * 계정 복구 처리
     * - delFlag를 false로 설정
     * - status를 ACTIVE로 변경
     * - deletedAt을 null로 초기화
     * 
     * 주의: 개인정보 마스킹은 복구되지 않습니다 (보안상의 이유)
     */
    public void restore() {
        this.delFlag = false;
        this.status = UserStatus.ACTIVE;
        this.deletedAt = null;
    }
}
