package com.app.replant.domain.mission.dto;

import com.app.replant.domain.mission.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissionRequest {

    @NotBlank(message = "미션 제목을 입력해주세요")
    @Size(max = 100, message = "제목은 100자 이하로 입력해주세요")
    private String title;

    @NotBlank(message = "미션 설명을 입력해주세요")
    private String description;

    // 기간: DAILY(일간), WEEKLY(주간), MONTHLY(월간)
    @NotNull(message = "미션 타입을 선택해주세요")
    private MissionType type;

    // 인증방식: TIMER(시간인증), GPS(GPS인증), COMMUNITY(커뮤인증)
    @NotNull(message = "인증 타입을 선택해주세요")
    private VerificationType verificationType;

    // GPS 인증용 필드
    private BigDecimal gpsLatitude;
    private BigDecimal gpsLongitude;
    private Integer gpsRadiusMeters;

    // 시간 인증용 필드
    private Integer requiredMinutes;

    // 보상 설정
    private Integer expReward;
    private Integer badgeDurationDays;

    // 미션 활성화 여부
    private Boolean isActive;

    // ============ 사용자 맞춤 필드들 ============

    // 고민 종류: RE_EMPLOYMENT(재취업), JOB_PREPARATION(취업준비), ENTRANCE_EXAM(입시),
    //          ADVANCEMENT(진학), RETURN_TO_SCHOOL(복학), RELATIONSHIP(연애), SELF_MANAGEMENT(자기관리)
    private WorryType worryType;

    // 연령대 (복수 선택 가능)
    private List<AgeRange> ageRanges;

    // 성별: MALE(남성), FEMALE(여성), ALL(전체)
    private GenderType genderType;

    // 지역: 광역자치단체 단위
    private RegionType regionType;

    // 장소: HOME(집), OUTDOOR(야외), INDOOR(실내)
    private PlaceType placeType;

    // 난이도: LEVEL1, LEVEL2, LEVEL3
    private DifficultyLevel difficultyLevel;
}
