package com.app.replant.domain.missionset.service;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.repository.MissionRepository;
import com.app.replant.domain.missionset.dto.TodoListDto;
import com.app.replant.domain.missionset.entity.TodoList;
import com.app.replant.domain.missionset.entity.TodoListMission;
import com.app.replant.domain.missionset.enums.MissionSource;
import com.app.replant.domain.missionset.enums.TodoListStatus;
import com.app.replant.domain.missionset.repository.TodoListLikeRepository;
import com.app.replant.domain.missionset.repository.TodoListMissionRepository;
import com.app.replant.domain.missionset.repository.TodoListRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import com.app.replant.domain.usermission.repository.UserMissionRepository;
import com.app.replant.domain.badge.repository.UserBadgeRepository;
import com.app.replant.domain.post.repository.PostRepository;
import com.app.replant.global.exception.CustomException;
import com.app.replant.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TodoListService {

        private final TodoListRepository todoListRepository;
        private final TodoListLikeRepository todoListLikeRepository;
        private final TodoListMissionRepository todoListMissionRepository;
        private final MissionRepository missionRepository;
        private final UserRepository userRepository;
        private final UserMissionRepository userMissionRepository;
        private final UserBadgeRepository userBadgeRepository;
        private final PostRepository postRepository;

        private static final int RANDOM_OFFICIAL_COUNT = 3; // 필수 공식 미션 개수

        /**
         * 투두리스트 초기화 - 랜덤 공식 미션 3개 조회
         */
        public TodoListDto.InitResponse initTodoList(Long userId) {
                // 랜덤 공식 미션 3개 조회
                List<Mission> randomMissions = missionRepository
                                .findRandomOfficialNonChallengeMissions(RANDOM_OFFICIAL_COUNT);

                if (randomMissions.size() < RANDOM_OFFICIAL_COUNT) {
                        log.warn("공식 미션이 {}개 미만입니다. 현재: {}개", RANDOM_OFFICIAL_COUNT, randomMissions.size());
                }

                return TodoListDto.InitResponse.builder()
                                .randomMissions(randomMissions.stream()
                                                .map(TodoListDto.MissionSimpleResponse::from)
                                                .collect(Collectors.toList()))
                                .build();
        }

        /**
         * 랜덤 미션 리롤 - 기존 미션을 제외하고 새로운 랜덤 미션 1개 조회
         */
        public TodoListDto.MissionSimpleResponse rerollRandomMission(Long userId, List<Long> excludeMissionIds) {
                Mission newMission = missionRepository
                                .findRandomOfficialNonChallengeMissionExcluding(excludeMissionIds)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND, 
                                                "교체할 수 있는 공식 미션이 없습니다."));
                
                return TodoListDto.MissionSimpleResponse.from(newMission);
        }

        /**
         * 커스텀 미션 도감 조회 (투두리스트 선택용)
         * 본인 것 + 다른 사람의 공개 커스텀 미션
         */
        public List<TodoListDto.MissionSimpleResponse> getSelectableMissions(Long userId, Boolean onlyMine, String searchQuery) {
                List<Mission> missions;
                
                if (Boolean.TRUE.equals(onlyMine)) {
                        // 본인 것만 조회
                        missions = missionRepository.findNonChallengeCustomMissionsByCreator(userId);
                } else {
                        // 본인이 만든 커스텀 미션
                        List<Mission> myMissions = missionRepository.findNonChallengeCustomMissionsByCreator(userId);
                        // 다른 사람들의 공개 커스텀 미션
                        List<Mission> publicMissions = missionRepository.findAllPublicNonChallengeCustomMissions();

                        // 중복 제거 (본인 것이 공개일 경우)
                        java.util.Set<Long> myMissionIds = myMissions.stream()
                                        .map(Mission::getId)
                                        .collect(java.util.stream.Collectors.toSet());

                        missions = new java.util.ArrayList<>(myMissions);
                        publicMissions.stream()
                                        .filter(m -> !myMissionIds.contains(m.getId()))
                                        .forEach(missions::add);
                }
                
                // 검색어 필터링
                if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                        String query = searchQuery.trim().toLowerCase();
                        missions = missions.stream()
                                        .filter(m -> m.getTitle().toLowerCase().contains(query) || 
                                                   (m.getDescription() != null && m.getDescription().toLowerCase().contains(query)))
                                        .collect(Collectors.toList());
                }

                return missions.stream()
                                .map(TodoListDto.MissionSimpleResponse::from)
                                .collect(Collectors.toList());
        }
        
        /**
         * 공개 투두리스트 목록 조회 (set_type=TODOLIST, is_public=true)
         */
        public Page<TodoListDto.SimpleResponse> getPublicTodoLists(Pageable pageable, String sortBy) {
                return todoListRepository.findPublicTodoLists(pageable, sortBy != null ? sortBy : "latest")
                                .map(TodoListDto.SimpleResponse::from);
        }

        /**
         * 공개 투두리스트 검색
         */
        public Page<TodoListDto.SimpleResponse> searchPublicTodoLists(String keyword, Pageable pageable, String sortBy) {
                return todoListRepository.searchPublicTodoLists(keyword, pageable, sortBy != null ? sortBy : "latest")
                                .map(TodoListDto.SimpleResponse::from);
        }

        /**
         * 공개 투두리스트 상세 조회 (공개된 것만 조회 가능).
         * 미션별로 작성자(creator)가 해당 미션에 대해 쓴 인증 게시글이 있으면 verificationPostId를 채워서 반환.
         */
        public TodoListDto.DetailResponse getPublicTodoListDetail(Long todoListId, Long userId) {
                TodoList todoList = todoListRepository.findTodoListByIdWithMissions(todoListId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));
                if (!Boolean.TRUE.equals(todoList.getIsPublic())) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }
                Long creatorId = todoList.getCreator() != null ? todoList.getCreator().getId() : null;
                List<TodoListDto.TodoMissionInfo> missionInfos = new java.util.ArrayList<>();
                if (todoList.getMissions() != null) {
                        for (TodoListMission msm : todoList.getMissions()) {
                                Long verificationPostId = null;
                                if (creatorId != null && msm.getMission() != null) {
                                        verificationPostId = postRepository.findVerificationPostByUserIdAndMissionId(creatorId, msm.getMission().getId())
                                                .map(p -> p.getId())
                                                .orElse(null);
                                }
                                missionInfos.add(TodoListDto.TodoMissionInfo.fromPublic(msm, verificationPostId));
                        }
                }
                return TodoListDto.DetailResponse.fromPublicDetail(todoList, missionInfos);
        }
        
        /**
         * 투두리스트 좋아요 (삭제된 기능 - 임시로 무시)
         */
        @Transactional
        public void likeTodoList(Long todoListId, Long userId) {
                // TODO: 좋아요 기능이 삭제되어 임시로 무시
                log.warn("좋아요 기능이 삭제되었습니다. todoListId={}, userId={}", todoListId, userId);
        }
        
        /**
         * 투두리스트 좋아요 취소 (삭제된 기능 - 임시로 무시)
         */
        @Transactional
        public void unlikeTodoList(Long todoListId, Long userId) {
                // TODO: 좋아요 기능이 삭제되어 임시로 무시
                log.warn("좋아요 취소 기능이 삭제되었습니다. todoListId={}, userId={}", todoListId, userId);
        }

        /**
         * 투두리스트 생성
         * 필수 공식 미션 3개 + 선택 커스텀 미션 (0개 이상)
         */
        @Transactional
        public TodoListDto.DetailResponse createTodoList(Long userId, TodoListDto.CreateRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                // 검증: 당일 이미 투두리스트가 생성되었는지 확인 (사용자 기준 오늘 = KST)
                ZoneId zoneSeoul = ZoneId.of("Asia/Seoul");
                LocalDateTime startOfDay = LocalDate.now(zoneSeoul).atStartOfDay();
                LocalDateTime endOfDay = startOfDay.plusDays(1);
                if (todoListRepository.existsByCreatorAndCreatedDate(user, startOfDay, endOfDay)) {
                        throw new CustomException(ErrorCode.TODO_DUPLICATE_DATE);
                }

                LocalDateTime now = LocalDateTime.now();

                // 검증: 필수 공식 미션 3개 필수
                if (request.getRandomMissionIds() == null
                                || request.getRandomMissionIds().size() != RANDOM_OFFICIAL_COUNT) {
                        throw new CustomException(ErrorCode.INVALID_REQUEST,
                                        "필수 공식 미션은 정확히 " + RANDOM_OFFICIAL_COUNT + "개여야 합니다.");
                }

                // 커스텀 미션은 0개 이상 허용 (null이면 빈 리스트로 처리)
                List<Long> customMissionIds = request.getCustomMissionIds() != null 
                                ? request.getCustomMissionIds() 
                                : java.util.Collections.emptyList();

                // 미션들 조회
                List<Mission> randomMissions = missionRepository.findByIdIn(request.getRandomMissionIds());
                List<Mission> customMissions = customMissionIds.isEmpty() 
                                ? java.util.Collections.emptyList()
                                : missionRepository.findByIdIn(customMissionIds);


                // 투두리스트 생성 (총 미션 수 = 필수 3개 + 커스텀 미션 개수)
                int totalMissionCount = RANDOM_OFFICIAL_COUNT + customMissions.size();
                TodoList todoList = TodoList.todoListBuilder()
                                .creator(user)
                                .title(request.getTitle() != null ? request.getTitle() : "나의 투두리스트")
                                .description(request.getDescription())
                                .totalCount(totalMissionCount)
                                .build();

                // 랜덤 공식 미션 추가
                int order = 0;
                LocalDateTime defaultDueDate = now.plusDays(7); // 기본 마감일 7일

                for (Mission mission : randomMissions) {
                        TodoListMission msm = TodoListMission.todoMissionBuilder()
                                        .todoList(todoList)
                                        .mission(mission)
                                        .displayOrder(order++)
                                        .missionSource(MissionSource.RANDOM_OFFICIAL)
                                        .build();
                        
                        // 시간대 정보가 있으면 설정
                        if (request.getMissionSchedules() != null) {
                                TodoListDto.CreateRequest.MissionScheduleInfo schedule = 
                                                request.getMissionSchedules().get(mission.getId());
                                if (schedule != null && schedule.getStartTime() != null && schedule.getEndTime() != null) {
                                        // 시간 검증: 시작 시간이 종료 시간보다 이전이어야 함
                                        if (!schedule.getStartTime().isBefore(schedule.getEndTime())) {
                                                throw new CustomException(ErrorCode.INVALID_REQUEST,
                                                                "미션 '" + mission.getTitle() + "'의 시작 시간은 종료 시간보다 이전이어야 합니다.");
                                        }
                                        msm.updateSchedule(schedule.getStartTime(), schedule.getEndTime());
                                }
                        }
                        
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
                        TodoListMission msm = TodoListMission.todoMissionBuilder()
                                        .todoList(todoList)
                                        .mission(mission)
                                        .displayOrder(order++)
                                        .missionSource(MissionSource.CUSTOM_SELECTED)
                                        .build();
                        
                        // 시간대 정보가 있으면 설정
                        if (request.getMissionSchedules() != null) {
                                TodoListDto.CreateRequest.MissionScheduleInfo schedule = 
                                                request.getMissionSchedules().get(mission.getId());
                                if (schedule != null && schedule.getStartTime() != null && schedule.getEndTime() != null) {
                                        // 시간 검증: 시작 시간이 종료 시간보다 이전이어야 함
                                        if (!schedule.getStartTime().isBefore(schedule.getEndTime())) {
                                                throw new CustomException(ErrorCode.INVALID_REQUEST,
                                                                "미션 '" + mission.getTitle() + "'의 시작 시간은 종료 시간보다 이전이어야 합니다.");
                                        }
                                        msm.updateSchedule(schedule.getStartTime(), schedule.getEndTime());
                                }
                        }
                        
                        todoList.getMissions().add(msm);

                        // UserMission 생성 - 투두리스트에 추가된 미션을 나의 미션에도 추가
                        LocalDateTime dueDate = mission.getDurationDays() != null
                                        ? now.plusDays(mission.getDurationDays())
                                        : (mission.getDeadlineDays() != null ? now.plusDays(mission.getDeadlineDays())
                                                        : defaultDueDate);
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

                todoListRepository.save(todoList);

                log.info("투두리스트 생성 완료: id={}, userId={}, 필수 미션 {}개, 커스텀 미션 {}개, 총 {}개",
                                todoList.getId(), userId, RANDOM_OFFICIAL_COUNT, customMissions.size(), totalMissionCount);
                return TodoListDto.DetailResponse.from(todoList, userId, userMissionRepository);
        }

        /**
         * 내 투두리스트 목록 조회
         */
        public Page<TodoListDto.SimpleResponse> getMyTodoLists(Long userId, Pageable pageable) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                return todoListRepository.findTodoListsByCreator(user, pageable)
                                .map(TodoListDto.SimpleResponse::from);
        }

        /**
         * 활성 투두리스트 목록 조회
         */
        public List<TodoListDto.SimpleResponse> getActiveTodoLists(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                return todoListRepository.findTodoListsByCreatorAndStatus(user, TodoListStatus.ACTIVE)
                                .stream()
                                .map(TodoListDto.SimpleResponse::from)
                                .collect(Collectors.toList());
        }

        /**
         * 투두리스트 상세 조회
         */
        public TodoListDto.DetailResponse getTodoListDetail(Long todoListId, Long userId) {
                TodoList todoList = todoListRepository.findTodoListByIdWithMissions(todoListId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

                // 본인만 조회 가능
                if (!todoList.isCreator(userId)) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }

                return TodoListDto.DetailResponse.from(todoList, userId, userMissionRepository);
        }

        /**
         * 투두리스트 미션 완료 처리
         */
        @Transactional
        public TodoListDto.DetailResponse completeMission(Long todoListId, Long missionId, Long userId) {
                TodoList todoList = todoListRepository.findTodoListByIdWithMissions(todoListId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

                // 본인만 완료 가능
                if (!todoList.isCreator(userId)) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }

                // 미션 찾기
                TodoListMission targetMission = todoList.getMissions().stream()
                                .filter(msm -> msm.getMission().getId().equals(missionId))
                                .findFirst()
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_IN_SET));

                // 이미 완료된 미션인지 확인
                if (targetMission.isCompletedMission()) {
                        throw new CustomException(ErrorCode.INVALID_REQUEST, "이미 완료된 미션입니다.");
                }

                Mission mission = targetMission.getMission();
                
                // 공식 미션인 경우: 인증 필수 (UserMission이 COMPLETED 상태여야 함)
                if (mission.isOfficialMission()) {
                        // 중복이 있을 수 있으므로 첫 번째 결과만 사용
                        List<UserMission> userMissions = userMissionRepository.findByUserIdAndMissionId(userId, missionId);
                        if (userMissions.isEmpty()) {
                                throw new CustomException(ErrorCode.USER_MISSION_NOT_FOUND);
                        }
                        UserMission userMission = userMissions.get(0);
                        
                        // UserMission이 COMPLETED 상태가 아니면 인증이 완료되지 않은 것
                        if (userMission.getStatus() != UserMissionStatus.COMPLETED) {
                                throw new CustomException(ErrorCode.VERIFICATION_REQUIRED);
                        }
                }
                
                // 커스텀 미션인 경우: 인증 없이 바로 완료 처리 가능
                // 미션 완료 처리
                targetMission.complete();

                // UserMission도 완료 처리 (커스텀 미션의 경우 ASSIGNED 상태에서 바로 COMPLETED로 변경)
                if (mission.isCustomMission()) {
                        // 중복이 있을 수 있으므로 첫 번째 결과만 사용
                        List<UserMission> userMissions = userMissionRepository.findByUserIdAndMissionIdAndStatusAssigned(userId, missionId);
                        if (!userMissions.isEmpty()) {
                                userMissions.get(0).complete();
                        }
                }

                // 투두리스트 완료 카운트 증가
                todoList.incrementCompletedCount();

                log.info("투두리스트 미션 완료: todoListId={}, missionId={}, completedCount={}",
                                todoListId, missionId, todoList.getCompletedCount());

                return TodoListDto.DetailResponse.from(todoList, userId, userMissionRepository);
        }

        /**
         * 새 투두리스트 생성 가능 여부 확인
         * 개수 제한 없음 (신규 가입자 포함, 모든 사용자가 생성 가능)
         */
        public boolean canCreateNewTodoList(Long userId) {
                // 개수 제한 없음 - 항상 생성 가능
                return true;
        }

        /**
         * 활성 투두리스트 개수 조회
         */
        public long getActiveTodoListCount(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                return todoListRepository.countActiveTodoListsByCreator(user);
        }

        /**
         * 투두리스트 보관처리
         */
        @Transactional
        public void archiveTodoList(Long todoListId, Long userId) {
                TodoList todoList = todoListRepository.findTodoListByIdWithMissions(todoListId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

                // 본인만 보관 가능
                if (!todoList.isCreator(userId)) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }

                todoList.archiveTodoList();
                log.info("투두리스트 보관 완료: todoListId={}, userId={}", todoListId, userId);
        }

        // ============ 공유 관련 메서드 제거됨 (불필요) ============
        // shareTodoList, unshareTodoList, getShareableTodoLists, getPublicTodoLists,
        // searchPublicTodoLists, getPublicTodoListDetail, copyTodoList

        // ============ 리뷰 관련 메서드들 ============

        /**
         * 투두리스트 수정
         */
        @Transactional
        public TodoListDto.DetailResponse updateTodoList(Long todoListId, Long userId,
                        TodoListDto.UpdateRequest request) {
                TodoList todoList = todoListRepository.findById(todoListId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

                // 생성자만 수정 가능
                if (!todoList.isCreator(userId)) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }

                todoList.update(request.getTitle(), request.getDescription());
                if (request.getIsPublic() != null) {
                        todoList.setPublic(request.getIsPublic());
                }

                log.info("투두리스트 수정 완료: id={}, userId={}", todoListId, userId);
                return TodoListDto.DetailResponse.from(todoList, userId, userMissionRepository);
        }


        /**
         * 투두리스트 삭제 (Hard Delete)
         * - 진행 중(ACTIVE)인 경우에만 삭제 가능
         * - 해당 투두리스트에 연결된 UserMission(나의 미션 진행중) 삭제 → 나의 미션에서도 사라지도록
         * - 연관 TodoListLike 삭제 후 TodoList 및 하위 TodoListMission 물리 삭제
         */
        @Transactional
        public void deleteTodoList(Long todoListId, Long userId) {
                TodoList todoList = todoListRepository.findTodoListByIdWithMissions(todoListId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

                if (!todoList.isCreator(userId)) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }

                if (todoList.getTodolistStatus() != TodoListStatus.ACTIVE) {
                        throw new CustomException(ErrorCode.TODO_LIST_DELETE_ONLY_ACTIVE);
                }

                // 나의 미션(진행중)에서도 제거: 해당 투두리스트와 같은 날·같은 미션의 ASSIGNED/PENDING UserMission 삭제
                List<Long> missionIds = todoList.getMissions().stream()
                                .map(msm -> msm.getMission().getId())
                                .collect(Collectors.toList());
                if (!missionIds.isEmpty()) {
                        ZoneId zone = ZoneId.of("Asia/Seoul");
                        LocalDate createdDate = todoList.getCreatedAt().atZone(zone).toLocalDate();
                        LocalDateTime startOfDay = createdDate.atStartOfDay();
                        LocalDateTime endOfDay = createdDate.plusDays(1).atStartOfDay();
                        List<UserMission> toDelete = userMissionRepository.findByUser_IdAndMission_IdInAndStatusInAndAssignedAtBetween(
                                        todoList.getCreator().getId(),
                                        missionIds,
                                        List.of(UserMissionStatus.ASSIGNED, UserMissionStatus.PENDING),
                                        startOfDay,
                                        endOfDay);
                        userMissionRepository.deleteAll(toDelete);
                        log.info("투두리스트 삭제: 연관 UserMission {}건 삭제 (나의 미션에서 제거)", toDelete.size());
                }

                todoListLikeRepository.deleteByTodoList(todoList);
                todoListRepository.delete(todoList);
                log.info("투두리스트 삭제 완료 (Hard Delete): todoListId={}, userId={}", todoListId, userId);
        }

        /**
         * 투두리스트에 미션 추가
         */
        @Transactional
        public TodoListDto.DetailResponse addMissionToTodoList(Long todoListId, Long userId, TodoListDto.AddMissionRequest request) {
                TodoList todoList = todoListRepository.findTodoListByIdWithMissions(todoListId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

                // 본인만 추가 가능
                if (!todoList.isCreator(userId)) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }

                Mission mission = missionRepository.findById(request.getMissionId())
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

                // 이미 존재하는 미션인지 확인
                if (todoListMissionRepository.existsByTodoListAndMission(todoList, mission)) {
                        throw new CustomException(ErrorCode.INVALID_REQUEST, "이미 추가된 미션입니다.");
                }

                // 최대 순서 조회
                Integer maxOrder = todoListMissionRepository.findMaxDisplayOrderByTodoList(todoList);

                TodoListMission msm = TodoListMission.todoMissionBuilder()
                                .todoList(todoList)
                                .mission(mission)
                                .displayOrder(maxOrder + 1)
                                .missionSource(MissionSource.CUSTOM_SELECTED)
                                .build();

                todoList.addMission(msm);
                todoListMissionRepository.save(msm);

                log.info("투두리스트에 미션 추가: todoListId={}, missionId={}", todoListId, request.getMissionId());
                return TodoListDto.DetailResponse.from(todoList, userId, userMissionRepository);
        }

        /**
         * 투두리스트에서 미션 제거
         */
        @Transactional
        public TodoListDto.DetailResponse removeMissionFromTodoList(Long todoListId, Long missionId, Long userId) {
                TodoList todoList = todoListRepository.findTodoListByIdWithMissions(todoListId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

                // 본인만 제거 가능
                if (!todoList.isCreator(userId)) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }

                Mission mission = missionRepository.findById(missionId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

                TodoListMission msm = todoListMissionRepository.findByTodoListAndMission(todoList, mission)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_IN_SET));

                todoList.removeMission(msm);
                todoListMissionRepository.delete(msm);

                log.info("투두리스트에서 미션 제거: todoListId={}, missionId={}", todoListId, missionId);
                return TodoListDto.DetailResponse.from(todoList, userId, userMissionRepository);
        }

        /**
         * 투두리스트 미션 순서 변경
         */
        @Transactional
        public TodoListDto.DetailResponse reorderMissions(Long todoListId, Long userId, TodoListDto.ReorderMissionsRequest request) {
                TodoList todoList = todoListRepository.findTodoListByIdWithMissions(todoListId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

                // 본인만 순서 변경 가능
                if (!todoList.isCreator(userId)) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }

                for (TodoListDto.ReorderMissionsRequest.MissionOrderItem item : request.getMissions()) {
                        todoList.getMissions().stream()
                                        .filter(msm -> msm.getMission().getId().equals(item.getMissionId()))
                                        .findFirst()
                                        .ifPresent(msm -> msm.updateDisplayOrder(item.getDisplayOrder()));
                }

                log.info("투두리스트 미션 순서 변경: todoListId={}", todoListId);
                return TodoListDto.DetailResponse.from(todoList, userId, userMissionRepository);
        }

        /**
         * 투두리스트 미션 시간대 설정
         */
        @Transactional
        public TodoListDto.DetailResponse updateMissionSchedule(Long todoListId, Long missionId, Long userId,
                        TodoListDto.UpdateMissionScheduleRequest request) {
                TodoList todoList = todoListRepository.findTodoListByIdWithMissions(todoListId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

                // 본인만 시간 설정 가능
                if (!todoList.isCreator(userId)) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }

                // 미션 찾기
                TodoListMission targetMission = todoList.getMissions().stream()
                                .filter(msm -> msm.getMission().getId().equals(missionId))
                                .findFirst()
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_IN_SET));

                // 시간 검증: 시작 시간이 종료 시간보다 이전이어야 함
                if (request.getStartTime() != null && request.getEndTime() != null) {
                        if (!request.getStartTime().isBefore(request.getEndTime())) {
                                throw new CustomException(ErrorCode.INVALID_REQUEST,
                                                "시작 시간은 종료 시간보다 이전이어야 합니다.");
                        }
                }

                // 시간 설정
                targetMission.updateSchedule(request.getStartTime(), request.getEndTime());

                log.info("투두리스트 미션 시간 설정: todoListId={}, missionId={}, startTime={}, endTime={}",
                                todoListId, missionId, request.getStartTime(), request.getEndTime());

                return TodoListDto.DetailResponse.from(todoList, userId, userMissionRepository);
        }
}
