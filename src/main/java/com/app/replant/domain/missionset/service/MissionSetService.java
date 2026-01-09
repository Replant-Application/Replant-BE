package com.app.replant.domain.missionset.service;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.repository.MissionRepository;
import com.app.replant.domain.missionset.dto.MissionSetDto;
import com.app.replant.domain.missionset.entity.MissionSet;
import com.app.replant.domain.missionset.entity.MissionSetMission;
import com.app.replant.domain.missionset.repository.MissionSetMissionRepository;
import com.app.replant.domain.missionset.repository.MissionSetRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MissionSetService {

    private final MissionSetRepository missionSetRepository;
    private final MissionSetMissionRepository missionSetMissionRepository;
    private final MissionRepository missionRepository;
    private final UserRepository userRepository;

    /**
     * 미션세트 생성
     */
    @Transactional
    public MissionSetDto.DetailResponse createMissionSet(Long userId, MissionSetDto.CreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        MissionSet missionSet = MissionSet.builder()
                .creator(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .isPublic(request.getIsPublic())
                .build();

        missionSetRepository.save(missionSet);

        // 미션 추가
        if (request.getMissionIds() != null && !request.getMissionIds().isEmpty()) {
            int order = 0;
            for (Long missionId : request.getMissionIds()) {
                Mission mission = missionRepository.findById(missionId)
                        .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

                MissionSetMission msm = MissionSetMission.builder()
                        .missionSet(missionSet)
                        .mission(mission)
                        .displayOrder(order++)
                        .build();

                missionSetMissionRepository.save(msm);
                missionSet.addMission(msm);
            }
        }

        log.info("미션세트 생성 완료: id={}, title={}, userId={}", missionSet.getId(), missionSet.getTitle(), userId);
        return MissionSetDto.DetailResponse.from(missionSet);
    }

    /**
     * 내 미션세트 목록 조회
     */
    public Page<MissionSetDto.SimpleResponse> getMyMissionSets(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return missionSetRepository.findByCreatorAndIsActive(user, true, pageable)
                .map(MissionSetDto.SimpleResponse::from);
    }

    /**
     * 공개 미션세트 목록 조회 (담은수 + 평점 순)
     */
    public Page<MissionSetDto.SimpleResponse> getPublicMissionSets(Pageable pageable) {
        return missionSetRepository.findPublicMissionSetsOrderByPopularity(pageable)
                .map(MissionSetDto.SimpleResponse::from);
    }

    /**
     * 공개 미션세트 검색
     */
    public Page<MissionSetDto.SimpleResponse> searchPublicMissionSets(String keyword, Pageable pageable) {
        return missionSetRepository.searchPublicMissionSets(keyword, pageable)
                .map(MissionSetDto.SimpleResponse::from);
    }

    /**
     * 미션세트 상세 조회
     */
    public MissionSetDto.DetailResponse getMissionSetDetail(Long missionSetId, Long userId) {
        MissionSet missionSet = missionSetRepository.findByIdWithMissions(missionSetId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

        // 비공개 미션세트는 생성자만 조회 가능
        if (!missionSet.getIsPublic() && !missionSet.isCreator(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        return MissionSetDto.DetailResponse.from(missionSet);
    }

    /**
     * 미션세트 수정
     */
    @Transactional
    public MissionSetDto.DetailResponse updateMissionSet(Long missionSetId, Long userId, MissionSetDto.UpdateRequest request) {
        MissionSet missionSet = missionSetRepository.findById(missionSetId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

        // 생성자만 수정 가능
        if (!missionSet.isCreator(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        missionSet.update(request.getTitle(), request.getDescription(), request.getIsPublic());

        log.info("미션세트 수정 완료: id={}, userId={}", missionSetId, userId);
        return MissionSetDto.DetailResponse.from(missionSet);
    }

    /**
     * 미션세트 삭제 (soft delete)
     */
    @Transactional
    public void deleteMissionSet(Long missionSetId, Long userId) {
        MissionSet missionSet = missionSetRepository.findById(missionSetId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

        // 생성자만 삭제 가능
        if (!missionSet.isCreator(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        missionSet.setActive(false);
        log.info("미션세트 삭제 완료: id={}, userId={}", missionSetId, userId);
    }

    /**
     * 미션세트에 미션 추가
     */
    @Transactional
    public MissionSetDto.DetailResponse addMissionToSet(Long missionSetId, Long userId, MissionSetDto.AddMissionRequest request) {
        MissionSet missionSet = missionSetRepository.findByIdWithMissions(missionSetId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

        // 생성자만 미션 추가 가능
        if (!missionSet.isCreator(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        Mission mission = missionRepository.findById(request.getMissionId())
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

        // 이미 추가된 미션인지 확인
        if (missionSetMissionRepository.existsByMissionSetAndMission(missionSet, mission)) {
            throw new CustomException(ErrorCode.MISSION_ALREADY_IN_SET);
        }

        Integer displayOrder = request.getDisplayOrder();
        if (displayOrder == null) {
            displayOrder = missionSetMissionRepository.findMaxDisplayOrderByMissionSet(missionSet) + 1;
        }

        MissionSetMission msm = MissionSetMission.builder()
                .missionSet(missionSet)
                .mission(mission)
                .displayOrder(displayOrder)
                .build();

        missionSetMissionRepository.save(msm);
        missionSet.addMission(msm);

        log.info("미션세트에 미션 추가: missionSetId={}, missionId={}", missionSetId, request.getMissionId());
        return MissionSetDto.DetailResponse.from(missionSet);
    }

    /**
     * 미션세트에서 미션 제거
     */
    @Transactional
    public MissionSetDto.DetailResponse removeMissionFromSet(Long missionSetId, Long missionId, Long userId) {
        MissionSet missionSet = missionSetRepository.findByIdWithMissions(missionSetId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

        // 생성자만 미션 제거 가능
        if (!missionSet.isCreator(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

        MissionSetMission msm = missionSetMissionRepository.findByMissionSetAndMission(missionSet, mission)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_IN_SET));

        missionSet.removeMission(msm);
        missionSetMissionRepository.delete(msm);

        log.info("미션세트에서 미션 제거: missionSetId={}, missionId={}", missionSetId, missionId);
        return MissionSetDto.DetailResponse.from(missionSet);
    }

    /**
     * 미션세트 미션 순서 변경
     */
    @Transactional
    public MissionSetDto.DetailResponse reorderMissions(Long missionSetId, Long userId, MissionSetDto.ReorderMissionsRequest request) {
        MissionSet missionSet = missionSetRepository.findByIdWithMissions(missionSetId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

        // 생성자만 순서 변경 가능
        if (!missionSet.isCreator(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        for (MissionSetDto.ReorderMissionsRequest.MissionOrderItem item : request.getMissions()) {
            Mission mission = missionRepository.findById(item.getMissionId())
                    .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

            MissionSetMission msm = missionSetMissionRepository.findByMissionSetAndMission(missionSet, mission)
                    .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_IN_SET));

            msm.updateDisplayOrder(item.getDisplayOrder());
        }

        log.info("미션세트 미션 순서 변경: missionSetId={}", missionSetId);
        return MissionSetDto.DetailResponse.from(missionSet);
    }

    /**
     * 미션세트 담기 (다른 사용자의 공개 미션세트 복사)
     */
    @Transactional
    public MissionSetDto.DetailResponse copyMissionSet(Long missionSetId, Long userId) {
        MissionSet originalSet = missionSetRepository.findByIdWithMissions(missionSetId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

        // 공개 미션세트만 담기 가능
        if (!originalSet.getIsPublic()) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 새 미션세트 생성
        MissionSet newMissionSet = MissionSet.builder()
                .creator(user)
                .title(originalSet.getTitle())
                .description(originalSet.getDescription())
                .isPublic(false)  // 복사한 미션세트는 기본 비공개
                .build();

        missionSetRepository.save(newMissionSet);

        // 미션 복사
        for (MissionSetMission originalMsm : originalSet.getMissions()) {
            MissionSetMission newMsm = MissionSetMission.builder()
                    .missionSet(newMissionSet)
                    .mission(originalMsm.getMission())
                    .displayOrder(originalMsm.getDisplayOrder())
                    .build();

            missionSetMissionRepository.save(newMsm);
            newMissionSet.addMission(newMsm);
        }

        // 원본 미션세트의 담은 수 증가
        originalSet.incrementAddedCount();

        log.info("미션세트 담기 완료: originalId={}, newId={}, userId={}", missionSetId, newMissionSet.getId(), userId);
        return MissionSetDto.DetailResponse.from(newMissionSet);
    }
}
