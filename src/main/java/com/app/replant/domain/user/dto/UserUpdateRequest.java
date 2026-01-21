package com.app.replant.domain.user.dto;

import com.app.replant.domain.mission.enums.PlaceType;
import com.app.replant.domain.mission.enums.WorryType;
import com.app.replant.domain.user.enums.Gender;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class UserUpdateRequest {
    private String nickname;
    private LocalDate birthDate;
    private Gender gender;
    private String profileImg;

    // ============ 사용자 맞춤 정보 필드들 ============
    // 고민 종류: RE_EMPLOYMENT(재취업), JOB_PREPARATION(취업준비), ENTRANCE_EXAM(입시),
    //          ADVANCEMENT(진학), RETURN_TO_SCHOOL(복학), RELATIONSHIP(연애)
    private WorryType worryType;
    // 지역 (서울, 경기, 인천 등)
    private String region;
    // 선호 장소: HOME(집), OUTDOOR(야외), INDOOR(실내)
    private PlaceType preferredPlaceType;
}
