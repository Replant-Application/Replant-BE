package com.app.replant.domain.missionset.service;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.mission.repository.MissionRepository;
import com.app.replant.domain.missionset.dto.TodoListDto;
import com.app.replant.domain.missionset.entity.MissionSet;
import com.app.replant.domain.missionset.entity.MissionSetMission;
import com.app.replant.domain.missionset.enums.MissionSetType;
import com.app.replant.domain.missionset.enums.MissionSource;
import com.app.replant.domain.missionset.enums.TodoListStatus;
import com.app.replant.domain.missionset.entity.MissionSetReview;
import com.app.replant.domain.missionset.repository.MissionSetMissionRepository;
import com.app.replant.domain.missionset.repository.MissionSetRepository;
import com.app.replant.domain.missionset.repository.MissionSetReviewRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.domain.usermission.entity.UserMission;
import com.app.replant.domain.usermission.enums.UserMissionStatus;
import com.app.replant.domain.usermission.repository.UserMissionRepository;
import com.app.replant.domain.badge.repository.UserBadgeRepository;
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
        private final MissionSetReviewRepository reviewRepository;
        private final MissionRepository missionRepository;
        private final UserRepository userRepository;
        private final UserMissionRepository userMissionRepository;
        private final UserBadgeRepository userBadgeRepository;

        private static final int RANDOM_OFFICIAL_COUNT = 3;
        private static final int CUSTOM_MISSION_COUNT = 2;
        private static final int TOTAL_MISSION_COUNT = 5;

        /**
         * 투두리스트 초기화 - 랜덤 공식 미션 3개 조회
         */
        public TodoListDto.InitResponse initTodoList(Long userId) {
                // 랜덤 비챌린지 공식 미션 3개 조회
                List<Mission> randomMissions = missionRepository
                                .findRandomOfficialNonChallengeMissions(RANDOM_OFFICIAL_COUNT);

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
                if (request.getRandomMissionIds() == null
                                || request.getRandomMissionIds().size() != RANDOM_OFFICIAL_COUNT) {
                        throw new CustomException(ErrorCode.INVALID_REQUEST,
                                        "랜덤 공식 미션은 정확히 " + RANDOM_OFFICIAL_COUNT + "개여야 합니다.");
                }
                if (request.getCustomMissionIds() == null
                                || request.getCustomMissionIds().size() != CUSTOM_MISSION_COUNT) {
                        throw new CustomException(ErrorCode.INVALID_REQUEST,
                                        "커스텀 미션은 정확히 " + CUSTOM_MISSION_COUNT + "개여야 합니다.");
                }

                // 미션들 조회
                List<Mission> randomMissions = missionRepository.findByIdIn(request.getRandomMissionIds());
                List<Mission> customMissions = missionRepository.findByIdIn(request.getCustomMissionIds());

                // 검증: 챌린지 미션이 포함되어 있지 않은지 확인
                for (Mission mission : randomMissions) {
                        if (Boolean.TRUE.equals(mission.getIsChallenge())) {
                                throw new CustomException(ErrorCode.INVALID_REQUEST,
                                                "챌린지 미션은 투두리스트에 추가할 수 없습니다: " + mission.getTitle());
                        }
                }
                for (Mission mission : customMissions) {
                        if (Boolean.TRUE.equals(mission.getIsChallenge())) {
                                throw new CustomException(ErrorCode.INVALID_REQUEST,
                                                "챌린지 미션은 투두리스트에 추가할 수 없습니다: " + mission.getTitle());
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
         * 최대 5개의 활성 투두리스트까지 생성 가능
         */
        public boolean canCreateNewTodoList(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                long activeTodoCount = missionSetRepository.countActiveTodoListsByCreator(user);

                // 최대 5개까지 허용
                return activeTodoCount < 5;
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

        // ============ 공개 투두리스트 관련 메서드들 ============

        /**
         * 공개 투두리스트 목록 조회 (정렬 옵션: popular, latest)
         */
        public Page<TodoListDto.PublicResponse> getPublicTodoLists(Pageable pageable, String sortBy) {
                if ("latest".equalsIgnoreCase(sortBy)) {
                        return missionSetRepository.findPublicMissionSetsOrderByLatest(pageable)
                                        .map(TodoListDto.PublicResponse::from);
                }
                // 기본값: 인기순 (popular)
                return missionSetRepository.findPublicMissionSetsOrderByPopularity(pageable)
                                .map(TodoListDto.PublicResponse::from);
        }

        /**
         * 공개 투두리스트 검색 (정렬 옵션: popular, latest)
         */
        public Page<TodoListDto.PublicResponse> searchPublicTodoLists(String keyword, Pageable pageable,
                        String sortBy) {
                if ("latest".equalsIgnoreCase(sortBy)) {
                        return missionSetRepository.searchPublicMissionSetsOrderByLatest(keyword, pageable)
                                        .map(TodoListDto.PublicResponse::from);
                }
                // 기본값: 인기순 (popular)
                return missionSetRepository.searchPublicMissionSetsOrderByPopularity(keyword, pageable)
                                .map(TodoListDto.PublicResponse::from);
        }

        /**
         * 공개 투두리스트 상세 조회
         */
        public TodoListDto.PublicDetailResponse getPublicTodoListDetail(Long todoListId) {
                MissionSet todoList = missionSetRepository.findByIdWithMissions(todoListId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

                // 비공개 투두리스트는 조회 불가
                if (!Boolean.TRUE.equals(todoList.getIsPublic())) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED, "비공개 투두리스트입니다.");
                }

                return TodoListDto.PublicDetailResponse.from(todoList);
        }

        /**
         * 투두리스트 담기 (다른 사용자의 공개 투두리스트 복사)
         */
        @Transactional
        public TodoListDto.DetailResponse copyTodoList(Long todoListId, Long userId) {
                MissionSet originalList = missionSetRepository.findByIdWithMissions(todoListId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

                // 공개 투두리스트만 담기 가능
                if (!Boolean.TRUE.equals(originalList.getIsPublic())) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED, "비공개 투두리스트는 담을 수 없습니다.");
                }

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                // 원본 투두리스트의 담은 수 증가
                originalList.incrementAddedCount();

                // 새 투두리스트 생성
                int missionCount = originalList.getMissions() != null ? originalList.getMissions().size() : 5;
                MissionSet newTodoList = MissionSet.todoListBuilder()
                                .creator(user)
                                .title(originalList.getTitle())
                                .description(originalList.getDescription())
                                .totalCount(missionCount)
                                .build();

                // 미션 복사
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime defaultDueDate = now.plusDays(7);

                for (MissionSetMission originalMsm : originalList.getMissions()) {
                        Mission mission = originalMsm.getMission();

                        MissionSetMission newMsm = MissionSetMission.todoMissionBuilder()
                                        .missionSet(newTodoList)
                                        .mission(mission)
                                        .displayOrder(originalMsm.getDisplayOrder())
                                        .missionSource(MissionSource.CUSTOM_SELECTED) // 담은 미션은 커스텀으로 표시
                                        .build();
                        newTodoList.getMissions().add(newMsm);

                        // UserMission 생성
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

                missionSetRepository.save(newTodoList);

                log.info("투두리스트 담기 완료: originalId={}, newId={}, userId={}", todoListId, newTodoList.getId(), userId);
                return TodoListDto.DetailResponse.from(newTodoList);
        }

        // ============ 리뷰 관련 메서드들 ============

        /**
         * 투두리스트 리뷰 작성
         */
        @Transactional
        public TodoListDto.ReviewResponse createReview(Long todoListId, Long userId,
                        TodoListDto.ReviewRequest request) {
                MissionSet todoList = missionSetRepository.findById(todoListId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

                // 비공개 투두리스트에는 리뷰 작성 불가
                if (!Boolean.TRUE.equals(todoList.getIsPublic())) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED, "비공개 투두리스트에는 리뷰를 작성할 수 없습니다.");
                }

                // 자신의 투두리스트에는 리뷰 작성 불가
                if (todoList.isCreator(userId)) {
                        throw new CustomException(ErrorCode.INVALID_REQUEST, "자신의 투두리스트에는 리뷰를 작성할 수 없습니다.");
                }

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                // 이미 리뷰를 작성했는지 확인
                if (reviewRepository.existsByMissionSetAndUser(todoList, user)) {
                        throw new CustomException(ErrorCode.INVALID_REQUEST, "이미 리뷰를 작성했습니다.");
                }

                // 뱃지 보유 여부 확인 (투두리스트 내 미션 중 하나라도 뱃지가 있어야 함)
                boolean hasBadge = todoList.getMissions().stream()
                                .anyMatch(msm -> userBadgeRepository.hasValidBadgeForMission(userId,
                                                msm.getMission().getId(), LocalDateTime.now()));

                if (!hasBadge) {
                        throw new CustomException(ErrorCode.BADGE_REQUIRED, "이 투두리스트의 미션 뱃지를 획득해야 리뷰를 작성할 수 있습니다.");
                }

                // 리뷰 생성
                MissionSetReview review = MissionSetReview.builder()
                                .missionSet(todoList)
                                .user(user)
                                .rating(request.getRating())
                                .content(request.getContent())
                                .build();
                reviewRepository.save(review);

                // 평균 별점 업데이트
                updateAverageRating(todoList);

                log.info("투두리스트 리뷰 작성: todoListId={}, userId={}", todoListId, userId);
                return buildReviewResponse(review);
        }

        /**
         * 투두리스트 리뷰 목록 조회
         */
        public Page<TodoListDto.ReviewResponse> getReviews(Long todoListId, Pageable pageable) {
                MissionSet todoList = missionSetRepository.findById(todoListId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

                return reviewRepository.findByMissionSetOrderByCreatedAtDesc(todoList, pageable)
                                .map(this::buildReviewResponse);
        }

        /**
         * 평균 별점 업데이트
         */
        private void updateAverageRating(MissionSet missionSet) {
                List<MissionSetReview> reviews = reviewRepository.findByMissionSet(missionSet);
                if (!reviews.isEmpty()) {
                        double avgRating = reviews.stream()
                                        .mapToInt(MissionSetReview::getRating)
                                        .average()
                                        .orElse(0.0);
                        missionSet.updateRating(avgRating, reviews.size());
                }
        }

        /**
         * ReviewResponse 빌드 헬퍼
         */
        private TodoListDto.ReviewResponse buildReviewResponse(MissionSetReview review) {
                return TodoListDto.ReviewResponse.builder()
                                .id(review.getId())
                                .todoListId(review.getMissionSet().getId())
                                .userId(review.getUser().getId())
                                .userNickname(review.getUser().getNickname())
                                .rating(review.getRating())
                                .content(review.getContent())
                                .createdAt(review.getCreatedAt())
                                .updatedAt(review.getUpdatedAt())
                                .build();
        }

        /**
         * 투두리스트 수정
         */
        @Transactional
        public TodoListDto.DetailResponse updateTodoList(Long todoListId, Long userId,
                        TodoListDto.UpdateRequest request) {
                MissionSet todoList = missionSetRepository.findById(todoListId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

                // 생성자만 수정 가능
                if (!todoList.isCreator(userId)) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }

                todoList.update(request.getTitle(), request.getDescription(), request.getIsPublic());

                log.info("투두리스트 수정 완료: id={}, userId={}", todoListId, userId);
                return TodoListDto.DetailResponse.from(todoList);
        }

        /**
         * 투두리스트 삭제 (soft delete)
         */
        @Transactional
        public void deleteTodoList(Long todoListId, Long userId) {
                MissionSet todoList = missionSetRepository.findById(todoListId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

                // 생성자만 삭제 가능
                if (!todoList.isCreator(userId)) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }

                todoList.setActive(false);
                log.info("투두리스트 삭제 완료: id={}, userId={}", todoListId, userId);
        }

        /**
         * 투두리스트에 미션 추가
         */
        @Transactional
        public TodoListDto.DetailResponse addMissionToTodoList(Long todoListId, Long userId,
                        TodoListDto.AddMissionRequest request) {
                MissionSet todoList = missionSetRepository.findByIdWithMissions(todoListId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

                // 생성자만 미션 추가 가능
                if (!todoList.isCreator(userId)) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }

                Mission mission = missionRepository.findById(request.getMissionId())
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

                // 이미 추가된 미션인지 확인
                if (missionSetMissionRepository.existsByMissionSetAndMission(todoList, mission)) {
                        throw new CustomException(ErrorCode.MISSION_ALREADY_IN_SET);
                }

                Integer displayOrder = request.getDisplayOrder();
                if (displayOrder == null) {
                        displayOrder = missionSetMissionRepository.findMaxDisplayOrderByMissionSet(todoList) + 1;
                }

                MissionSetMission msm = MissionSetMission.builder()
                                .missionSet(todoList)
                                .mission(mission)
                                .displayOrder(displayOrder)
                                .build();

                // cascade로 저장됨
                todoList.getMissions().add(msm);

                log.info("투두리스트에 미션 추가: todoListId={}, missionId={}", todoListId, request.getMissionId());
                return TodoListDto.DetailResponse.from(todoList);
        }

        /**
         * 투두리스트에서 미션 제거
         */
        @Transactional
        public TodoListDto.DetailResponse removeMissionFromTodoList(Long todoListId, Long missionId, Long userId) {
                MissionSet todoList = missionSetRepository.findByIdWithMissions(todoListId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

                // 생성자만 미션 제거 가능
                if (!todoList.isCreator(userId)) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }

                Mission mission = missionRepository.findById(missionId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

                MissionSetMission msm = missionSetMissionRepository.findByMissionSetAndMission(todoList, mission)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_IN_SET));

                todoList.removeMission(msm);
                missionSetMissionRepository.delete(msm);

                log.info("투두리스트에서 미션 제거: todoListId={}, missionId={}", todoListId, missionId);
                return TodoListDto.DetailResponse.from(todoList);
        }

        /**
         * 투두리스트 미션 순서 변경
         */
        @Transactional
        public TodoListDto.DetailResponse reorderMissions(Long todoListId, Long userId,
                        TodoListDto.ReorderMissionsRequest request) {
                MissionSet todoList = missionSetRepository.findByIdWithMissions(todoListId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

                // 생성자만 순서 변경 가능
                if (!todoList.isCreator(userId)) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }

                for (TodoListDto.ReorderMissionsRequest.MissionOrderItem item : request.getMissions()) {
                        Mission mission = missionRepository.findById(item.getMissionId())
                                        .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

                        MissionSetMission msm = missionSetMissionRepository
                                        .findByMissionSetAndMission(todoList, mission)
                                        .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_IN_SET));

                        msm.updateDisplayOrder(item.getDisplayOrder());
                }

                log.info("투두리스트 미션 순서 변경: todoListId={}", todoListId);
                return TodoListDto.DetailResponse.from(todoList);
        }

        /**
         * 내 리뷰 조회
         */
        public TodoListDto.ReviewResponse getMyReview(Long todoListId, Long userId) {
                MissionSet todoList = missionSetRepository.findById(todoListId)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                MissionSetReview review = reviewRepository.findByMissionSetAndUser(todoList, user)
                                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

                return buildReviewResponse(review);
        }

        /**
         * 리뷰 수정
         */
        @Transactional
        public TodoListDto.ReviewResponse updateReview(Long reviewId, Long userId,
                        TodoListDto.UpdateReviewRequest request) {
                MissionSetReview review = reviewRepository.findById(reviewId)
                                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

                // 작성자만 수정 가능
                if (!review.getUser().getId().equals(userId)) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }

                review.update(request.getRating(), request.getContent());

                // 평균 별점 재계산
                updateAverageRating(review.getMissionSet());

                log.info("리뷰 수정 완료: reviewId={}, userId={}", reviewId, userId);
                return buildReviewResponse(review);
        }

        /**
         * 리뷰 삭제
         */
        @Transactional
        public void deleteReview(Long reviewId, Long userId) {
                MissionSetReview review = reviewRepository.findById(reviewId)
                                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

                // 작성자만 삭제 가능
                if (!review.getUser().getId().equals(userId)) {
                        throw new CustomException(ErrorCode.ACCESS_DENIED);
                }

                MissionSet todoList = review.getMissionSet();
                reviewRepository.delete(review);

                // 평균 별점 재계산
                updateAverageRating(todoList);

                log.info("리뷰 삭제 완료: reviewId={}, userId={}", reviewId, userId);
        }
}
