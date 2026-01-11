package com.app.replant.domain.missionset.service;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.repository.MissionRepository;
import com.app.replant.domain.missionset.dto.TodoListDto;
import com.app.replant.domain.missionset.entity.MissionSet;
import com.app.replant.domain.missionset.entity.MissionSetMission;
import com.app.replant.domain.missionset.enums.MissionSetType;
import com.app.replant.domain.missionset.enums.MissionSource;
import com.app.replant.domain.missionset.enums.TodoListStatus;
import com.app.replant.domain.missionset.repository.MissionSetMissionRepository;
import com.app.replant.domain.missionset.repository.MissionSetRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import com.app.replant.domain.usermission.repository.UserMissionRepository;
import com.app.replant.exception.CustomException;
import com.app.replant.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TodoListService {

    private final MissionSetRepository missionSetRepository;
    private final MissionSetMissionRepository missionSetMissionRepository;
    private final MissionRepository missionRepository;
    private final UserRepository userRepository;
    private final UserMissionRepository userMissionRepository;

    private static final int RANDOM_OFFICIAL_COUNT = 3;
    private static final int CUSTOM_MISSION_COUNT = 2;
    private static final int TOTAL_MISSION_COUNT = 5;

    /**
     * 투두리스트 초기화 - 랜덤 공식 미션 3개 조회
     */
    public TodoListDto.InitResponse initTodoList(Long userId) {
        // 랜덤 비챌린지 공식 미션 3개 조회
        List<Mission> randomMissions = missionRepository.findRandomOfficialNonChallengeMissions(RANDOM_OFFICIAL_COUNT);

        if (randomMissions.size() < RANDOM_OFFICIAL_COUNT) {
            log.warn("비챌린지 공식 미션이 {}개 미만입니다. 현재: {}개", RANDOM_OFFICIAL_COUNT, randomMissions.size());
        }

        return TodoListDto.InitResponse.builder()
                .randomMissions(randomMissions.stream()
                        .map(TodoListDto.MissionSimpleResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * 커스텀 미션 도감 조회 (투두리스트 선택용)
     * 챌린지가 아닌 커스텀 미션만 조회
     */
    public List<TodoListDto.MissionSimpleResponse> getSelectableMissions(Long userId) {
        List<Mission> customMissions = missionRepository.findNonChallengeCustomMissionsByCreator(userId);

        return customMissions.stream()
                .map(TodoListDto.MissionSimpleResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 투두리스트 생성
     * 랜덤 공식 미션 3개 + 사용자 선택 커스텀 미션 2개
     */
    @Transactional
    public TodoListDto.DetailResponse createTodoList(Long userId, TodoListDto.CreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 검증: 랜덤 미션 3개, 커스텀 미션 2개
        if (request.getRandomMissionIds() == null || request.getRandomMissionIds().size() != RANDOM_OFFICIAL_COUNT) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "랜덤 공식 미션은 정확히 " + RANDOM_OFFICIAL_COUNT + "개여야 합니다.");
        }
        if (request.getCustomMissionIds() == null || request.getCustomMissionIds().size() != CUSTOM_MISSION_COUNT) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "커스텀 미션은 정확히 " + CUSTOM_MISSION_COUNT + "개여야 합니다.");
        }

        // 미션들 조회
        List<Mission> randomMissions = missionRepository.findByIdIn(request.getRandomMissionIds());
        List<Mission> customMissions = missionRepository.findByIdIn(request.getCustomMissionIds());

        // 검증: 챌린지 미션이 포함되어 있지 않은지 확인
        for (Mission mission : randomMissions) {
            if (Boolean.TRUE.equals(mission.getIsChallenge())) {
                throw new CustomException(ErrorCode.INVALID_REQUEST, "챌린지 미션은 투두리스트에 추가할 수 없습니다: " + mission.getTitle());
            }
        }
        for (Mission mission : customMissions) {
            if (Boolean.TRUE.equals(mission.getIsChallenge())) {
                throw new CustomException(ErrorCode.INVALID_REQUEST, "챌린지 미션은 투두리스트에 추가할 수 없습니다: " + mission.getTitle());
            }
        }

        // 투두리스트 생성
        MissionSet todoList = MissionSet.todoListBuilder()
                .creator(user)
                .title(request.getTitle() != null ? request.getTitle() : "나의 투두리스트")
                .description(request.getDescription())
                .totalCount(TOTAL_MISSION_COUNT)
                .build();

        // 랜덤 공식 미션 추가
        int order = 0;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime defaultDueDate = now.plusDays(7); // 기본 마감일 7일

        for (Mission mission : randomMissions) {
            MissionSetMission msm = MissionSetMission.todoMissionBuilder()
                    .missionSet(todoList)
                    .mission(mission)
                    .displayOrder(order++)
                    .missionSource(MissionSource.RANDOM_OFFICIAL)
                    .build();
            todoList.getMissions().add(msm);

            // UserMission 생성 - 투두리스트에 추가된 미션을 나의 미션에도 추가
            LocalDateTime dueDate = mission.getDurationDays() != null
                    ? now.plusDays(mission.getDurationDays())
                    : defaultDueDate;
            UserMission userMission = UserMission.builder()
                    .user(user)
                    .mission(mission)
                    .missionType(mission.getMissionType())
                    .assignedAt(now)
                    .dueDate(dueDate)
                    .status(UserMissionStatus.ASSIGNED)
                    .build();
            userMissionRepository.save(userMission);
        }

        // 커스텀 미션 추가
        for (Mission mission : customMissions) {
            MissionSetMission msm = MissionSetMission.todoMissionBuilder()
                    .missionSet(todoList)
                    .mission(mission)
                    .displayOrder(order++)
                    .missionSource(MissionSource.CUSTOM_SELECTED)
                    .build();
            todoList.getMissions().add(msm);

            // UserMission 생성 - 투두리스트에 추가된 미션을 나의 미션에도 추가
            LocalDateTime dueDate = mission.getDurationDays() != null
                    ? now.plusDays(mission.getDurationDays())
                    : (mission.getDeadlineDays() != null ? now.plusDays(mission.getDeadlineDays()) : defaultDueDate);
            UserMission userMission = UserMission.builder()
                    .user(user)
                    .mission(mission)
                    .missionType(mission.getMissionType())
                    .assignedAt(now)
                    .dueDate(dueDate)
                    .status(UserMissionStatus.ASSIGNED)
                    .build();
            userMissionRepository.save(userMission);
        }

        missionSetRepository.save(todoList);

        log.info("투두리스트 생성 완료: id={}, userId={}, UserMission {}개 생성됨",
                todoList.getId(), userId, TOTAL_MISSION_COUNT);
        return TodoListDto.DetailResponse.from(todoList);
    }

    /**
     * 내 투두리스트 목록 조회
     */
    public Page<TodoListDto.SimpleResponse> getMyTodoLists(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return missionSetRepository.findTodoListsByCreator(user, pageable)
                .map(TodoListDto.SimpleResponse::from);
    }

    /**
     * 활성 투두리스트 목록 조회
     */
    public List<TodoListDto.SimpleResponse> getActiveTodoLists(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return missionSetRepository.findTodoListsByCreatorAndStatus(user, TodoListStatus.ACTIVE)
                .stream()
                .map(TodoListDto.SimpleResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 투두리스트 상세 조회
     */
    public TodoListDto.DetailResponse getTodoListDetail(Long todoListId, Long userId) {
        MissionSet todoList = missionSetRepository.findTodoListByIdWithMissions(todoListId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

        // 본인만 조회 가능
        if (!todoList.isCreator(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        return TodoListDto.DetailResponse.from(todoList);
    }

    /**
     * 투두리스트 미션 완료 처리
     */
    @Transactional
    public TodoListDto.DetailResponse completeMission(Long todoListId, Long missionId, Long userId) {
        MissionSet todoList = missionSetRepository.findTodoListByIdWithMissions(todoListId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

        // 본인만 완료 가능
        if (!todoList.isCreator(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 미션 찾기
        MissionSetMission targetMission = todoList.getMissions().stream()
                .filter(msm -> msm.getMission().getId().equals(missionId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_IN_SET));

        // 이미 완료된 미션인지 확인
        if (targetMission.isCompletedMission()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "이미 완료된 미션입니다.");
        }

        // 미션 완료 처리
        targetMission.complete();

        // UserMission도 완료 처리
        userMissionRepository.findByUserIdAndMissionIdAndStatusAssigned(userId, missionId)
                .ifPresent(UserMission::complete);

        // 투두리스트 완료 카운트 증가
        todoList.incrementCompletedCount();

        log.info("투두리스트 미션 완료: todoListId={}, missionId={}, completedCount={}",
                todoListId, missionId, todoList.getCompletedCount());

        return TodoListDto.DetailResponse.from(todoList);
    }

    /**
     * 새 투두리스트 생성 가능 여부 확인
     */
    public boolean canCreateNewTodoList(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<MissionSet> activeTodoLists = missionSetRepository.findTodoListsByCreatorAndStatus(user, TodoListStatus.ACTIVE);

        // 활성 투두리스트가 없거나, 모든 활성 투두리스트가 80% 이상 완료된 경우 생성 가능
        if (activeTodoLists.isEmpty()) {
            return true;
        }

        return activeTodoLists.stream().allMatch(MissionSet::canCreateNewTodoList);
    }

    /**
     * 활성 투두리스트 개수 조회
     */
    public long getActiveTodoListCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return missionSetRepository.countActiveTodoListsByCreator(user);
    }

    /**
     * 투두리스트 보관처리
     */
    @Transactional
    public void archiveTodoList(Long todoListId, Long userId) {
        MissionSet todoList = missionSetRepository.findTodoListByIdWithMissions(todoListId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

        // 본인만 보관 가능
        if (!todoList.isCreator(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        todoList.archiveTodoList();
        log.info("투두리스트 보관 완료: todoListId={}, userId={}", todoListId, userId);
    }

    /**
     * 투두리스트 공유 (isPublic = true로 전환)
     * 투두 공유 탭에서 내 투두리스트를 선택하여 공개로 전환
     */
    @Transactional
    public TodoListDto.DetailResponse shareTodoList(Long todoListId, Long userId) {
        MissionSet todoList = missionSetRepository.findByIdForShare(todoListId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

        // 본인만 공유 가능
        if (!todoList.isCreator(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 이미 공개된 투두리스트인지 확인
        if (Boolean.TRUE.equals(todoList.getIsPublic())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "이미 공유된 투두리스트입니다.");
        }

        // 공개로 전환
        todoList.update(null, null, true);
        log.info("투두리스트 공유 완료: todoListId={}, userId={}", todoListId, userId);

        return TodoListDto.DetailResponse.from(todoList);
    }

    /**
     * 투두리스트 공유 해제 (isPublic = false로 전환)
     */
    @Transactional
    public TodoListDto.DetailResponse unshareTodoList(Long todoListId, Long userId) {
        MissionSet todoList = missionSetRepository.findByIdForShare(todoListId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

        // 본인만 공유 해제 가능
        if (!todoList.isCreator(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 비공개로 전환
        todoList.update(null, null, false);
        log.info("투두리스트 공유 해제 완료: todoListId={}, userId={}", todoListId, userId);

        return TodoListDto.DetailResponse.from(todoList);
    }

    /**
     * 공유 가능한 내 투두리스트 목록 조회 (비공개 상태인 것만)
     */
    public List<TodoListDto.SimpleResponse> getShareableTodoLists(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return missionSetRepository.findPrivateMissionSetsByCreator(user)
                .stream()
                .map(TodoListDto.SimpleResponse::from)
                .collect(Collectors.toList());
    }
}
