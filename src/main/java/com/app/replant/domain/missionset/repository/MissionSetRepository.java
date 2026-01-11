package com.app.replant.domain.missionset.repository;

import com.app.replant.domain.missionset.entity.MissionSet;
import com.app.replant.domain.missionset.enums.MissionSetType;
import com.app.replant.domain.missionset.enums.TodoListStatus;
import com.app.replant.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MissionSetRepository extends JpaRepository<MissionSet, Long> {

    // 내가 만든 미션세트 목록
    List<MissionSet> findByCreatorAndIsActiveOrderByCreatedAtDesc(User creator, Boolean isActive);

    // 내가 만든 미션세트 목록 (페이징)
    Page<MissionSet> findByCreatorAndIsActive(User creator, Boolean isActive, Pageable pageable);

    // 공개된 미션세트 목록 (담은수 + 평점 순 정렬) - 인기순
    @Query("SELECT ms FROM MissionSet ms WHERE ms.isPublic = true AND ms.isActive = true " +
           "ORDER BY ms.addedCount DESC, ms.averageRating DESC, ms.createdAt DESC")
    Page<MissionSet> findPublicMissionSetsOrderByPopularity(Pageable pageable);

    // 공개된 미션세트 목록 (최신순 정렬)
    @Query("SELECT ms FROM MissionSet ms WHERE ms.isPublic = true AND ms.isActive = true " +
           "ORDER BY ms.createdAt DESC")
    Page<MissionSet> findPublicMissionSetsOrderByLatest(Pageable pageable);

    // 공개된 미션세트 검색 (인기순)
    @Query("SELECT ms FROM MissionSet ms WHERE ms.isPublic = true AND ms.isActive = true " +
           "AND (ms.title LIKE %:keyword% OR ms.description LIKE %:keyword%) " +
           "ORDER BY ms.addedCount DESC, ms.averageRating DESC")
    Page<MissionSet> searchPublicMissionSetsOrderByPopularity(@Param("keyword") String keyword, Pageable pageable);

    // 공개된 미션세트 검색 (최신순)
    @Query("SELECT ms FROM MissionSet ms WHERE ms.isPublic = true AND ms.isActive = true " +
           "AND (ms.title LIKE %:keyword% OR ms.description LIKE %:keyword%) " +
           "ORDER BY ms.createdAt DESC")
    Page<MissionSet> searchPublicMissionSetsOrderByLatest(@Param("keyword") String keyword, Pageable pageable);

    // 특정 사용자의 공개 미션세트
    Page<MissionSet> findByCreatorAndIsPublicAndIsActive(User creator, Boolean isPublic, Boolean isActive, Pageable pageable);

    // 미션세트 상세 조회 (미션 목록 포함)
    @Query("SELECT DISTINCT ms FROM MissionSet ms " +
           "LEFT JOIN FETCH ms.missions msm " +
           "LEFT JOIN FETCH msm.mission " +
           "WHERE ms.id = :id AND ms.isActive = true")
    Optional<MissionSet> findByIdWithMissions(@Param("id") Long id);

    // ============================================
    // 투두리스트용 쿼리
    // ============================================

    // 사용자의 투두리스트 목록 (상태별)
    @Query("SELECT ms FROM MissionSet ms WHERE ms.creator = :creator AND ms.setType = :setType " +
           "AND ms.isActive = true ORDER BY ms.createdAt DESC")
    List<MissionSet> findTodoListsByCreator(@Param("creator") User creator, @Param("setType") MissionSetType setType);

    // 사용자의 활성 투두리스트 목록
    @Query("SELECT ms FROM MissionSet ms WHERE ms.creator = :creator AND ms.setType = 'TODOLIST' " +
           "AND ms.todolistStatus = :status AND ms.isActive = true ORDER BY ms.createdAt DESC")
    List<MissionSet> findTodoListsByCreatorAndStatus(
        @Param("creator") User creator, 
        @Param("status") TodoListStatus status
    );

    // 사용자의 모든 투두리스트 (페이징)
    @Query("SELECT ms FROM MissionSet ms WHERE ms.creator = :creator AND ms.setType = 'TODOLIST' " +
           "AND ms.isActive = true ORDER BY ms.createdAt DESC")
    Page<MissionSet> findTodoListsByCreator(@Param("creator") User creator, Pageable pageable);

    // 사용자의 활성 투두리스트 개수
    @Query("SELECT COUNT(ms) FROM MissionSet ms WHERE ms.creator = :creator AND ms.setType = 'TODOLIST' " +
           "AND ms.todolistStatus = 'ACTIVE' AND ms.isActive = true")
    long countActiveTodoListsByCreator(@Param("creator") User creator);

    // 투두리스트 상세 조회 (미션 목록 포함)
    @Query("SELECT DISTINCT ms FROM MissionSet ms " +
           "LEFT JOIN FETCH ms.missions msm " +
           "LEFT JOIN FETCH msm.mission " +
           "WHERE ms.id = :id AND ms.setType = 'TODOLIST' AND ms.isActive = true")
    Optional<MissionSet> findTodoListByIdWithMissions(@Param("id") Long id);

    // 공유 가능한 투두리스트 조회 (비공개 상태인 것만)
    @Query("SELECT ms FROM MissionSet ms WHERE ms.creator = :creator AND ms.setType = 'TODOLIST' " +
           "AND ms.isPublic = false AND ms.isActive = true ORDER BY ms.createdAt DESC")
    List<MissionSet> findPrivateTodoListsByCreator(@Param("creator") User creator);

    // ID로 미션세트 조회 (setType 필터링 없음) - 공유/공유해제용
    @Query("SELECT DISTINCT ms FROM MissionSet ms " +
           "LEFT JOIN FETCH ms.missions msm " +
           "LEFT JOIN FETCH msm.mission " +
           "WHERE ms.id = :id AND ms.isActive = true")
    Optional<MissionSet> findByIdForShare(@Param("id") Long id);

    // 비공개 미션세트 목록 조회 (setType 필터링 없음)
    @Query("SELECT ms FROM MissionSet ms WHERE ms.creator = :creator " +
           "AND ms.isPublic = false AND ms.isActive = true ORDER BY ms.createdAt DESC")
    List<MissionSet> findPrivateMissionSetsByCreator(@Param("creator") User creator);
}
