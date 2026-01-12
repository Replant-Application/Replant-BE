package com.app.replant.domain.missionset.repository;

import com.app.replant.domain.missionset.entity.TodoList;
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
public interface TodoListRepository extends JpaRepository<TodoList, Long> {

    // 내가 만든 투두리스트 목록
    List<TodoList> findByCreatorAndIsActiveOrderByCreatedAtDesc(User creator, Boolean isActive);

    // 내가 만든 투두리스트 목록 (페이징)
    Page<TodoList> findByCreatorAndIsActive(User creator, Boolean isActive, Pageable pageable);

    // 공개된 투두리스트 목록 (담은수 + 평점 순 정렬) - 인기순
    @Query("SELECT tl FROM TodoList tl WHERE tl.isPublic = true AND tl.isActive = true " +
            "ORDER BY tl.addedCount DESC, tl.averageRating DESC, tl.createdAt DESC")
    Page<TodoList> findPublicTodoListsOrderByPopularity(Pageable pageable);

    // 공개된 투두리스트 목록 (최신순 정렬)
    @Query("SELECT tl FROM TodoList tl WHERE tl.isPublic = true AND tl.isActive = true " +
            "ORDER BY tl.createdAt DESC")
    Page<TodoList> findPublicTodoListsOrderByLatest(Pageable pageable);

    // 공개된 투두리스트 검색 (인기순)
    @Query("SELECT tl FROM TodoList tl WHERE tl.isPublic = true AND tl.isActive = true " +
            "AND (tl.title LIKE %:keyword% OR tl.description LIKE %:keyword%) " +
            "ORDER BY tl.addedCount DESC, tl.averageRating DESC")
    Page<TodoList> searchPublicTodoListsOrderByPopularity(@Param("keyword") String keyword, Pageable pageable);

    // 공개된 투두리스트 검색 (최신순)
    @Query("SELECT tl FROM TodoList tl WHERE tl.isPublic = true AND tl.isActive = true " +
            "AND (tl.title LIKE %:keyword% OR tl.description LIKE %:keyword%) " +
            "ORDER BY tl.createdAt DESC")
    Page<TodoList> searchPublicTodoListsOrderByLatest(@Param("keyword") String keyword, Pageable pageable);

    // 특정 사용자의 공개 투두리스트
    Page<TodoList> findByCreatorAndIsPublicAndIsActive(User creator, Boolean isPublic, Boolean isActive,
            Pageable pageable);

    // 투두리스트 상세 조회 (미션 목록 포함)
    @Query("SELECT DISTINCT tl FROM TodoList tl " +
            "LEFT JOIN FETCH tl.missions tlm " +
            "LEFT JOIN FETCH tlm.mission " +
            "WHERE tl.id = :id AND tl.isActive = true")
    Optional<TodoList> findByIdWithMissions(@Param("id") Long id);

    // ============================================
    // 투두리스트용 쿼리
    // ============================================

    // 사용자의 투두리스트 목록 (상태별)
    @Query("SELECT tl FROM TodoList tl WHERE tl.creator = :creator AND tl.setType = :setType " +
            "AND tl.isActive = true ORDER BY tl.createdAt DESC")
    List<TodoList> findTodoListsByCreator(@Param("creator") User creator,
            @Param("setType") MissionSetType setType);

    // 사용자의 활성 투두리스트 목록
    @Query("SELECT tl FROM TodoList tl WHERE tl.creator = :creator AND tl.setType = 'TODOLIST' " +
            "AND tl.todolistStatus = :status AND tl.isActive = true ORDER BY tl.createdAt DESC")
    List<TodoList> findTodoListsByCreatorAndStatus(
            @Param("creator") User creator,
            @Param("status") TodoListStatus status);

    // 사용자의 모든 투두리스트 (페이징)
    @Query("SELECT tl FROM TodoList tl WHERE tl.creator = :creator AND tl.setType = 'TODOLIST' " +
            "AND tl.isActive = true ORDER BY tl.createdAt DESC")
    Page<TodoList> findTodoListsByCreator(@Param("creator") User creator, Pageable pageable);

    // 사용자의 활성 투두리스트 개수
    @Query("SELECT COUNT(tl) FROM TodoList tl WHERE tl.creator = :creator AND tl.setType = 'TODOLIST' " +
            "AND tl.todolistStatus = 'ACTIVE' AND tl.isActive = true")
    long countActiveTodoListsByCreator(@Param("creator") User creator);

    // 사용자의 모든 투두리스트 개수 (상태 무관, 기존 가입자 판단용)
    @Query("SELECT COUNT(tl) FROM TodoList tl WHERE tl.creator = :creator AND tl.setType = 'TODOLIST' " +
            "AND tl.isActive = true")
    long countAllTodoListsByCreator(@Param("creator") User creator);

    // 투두리스트 상세 조회 (미션 목록 포함)
    @Query("SELECT DISTINCT tl FROM TodoList tl " +
            "LEFT JOIN FETCH tl.missions tlm " +
            "LEFT JOIN FETCH tlm.mission " +
            "WHERE tl.id = :id AND tl.setType = 'TODOLIST' AND tl.isActive = true")
    Optional<TodoList> findTodoListByIdWithMissions(@Param("id") Long id);

    // 공유 가능한 투두리스트 조회 (비공개 상태인 것만)
    @Query("SELECT tl FROM TodoList tl WHERE tl.creator = :creator AND tl.setType = 'TODOLIST' " +
            "AND tl.isPublic = false AND tl.isActive = true ORDER BY tl.createdAt DESC")
    List<TodoList> findPrivateTodoListsByCreator(@Param("creator") User creator);

    // ID로 투두리스트 조회 (setType 필터링 없음) - 공유/공유해제용
    @Query("SELECT DISTINCT tl FROM TodoList tl " +
            "LEFT JOIN FETCH tl.missions tlm " +
            "LEFT JOIN FETCH tlm.mission " +
            "WHERE tl.id = :id AND tl.isActive = true")
    Optional<TodoList> findByIdForShare(@Param("id") Long id);

    // 비공개 투두리스트 목록 조회 (setType 필터링 없음)
    @Query("SELECT tl FROM TodoList tl WHERE tl.creator = :creator " +
            "AND tl.isPublic = false AND tl.isActive = true ORDER BY tl.createdAt DESC")
    List<TodoList> findPrivateTodoListsByCreatorV2(@Param("creator") User creator);

    /**
     * 모든 활성 투두리스트 조회 (만료 처리용)
     */
    @Query("SELECT DISTINCT tl FROM TodoList tl " +
            "LEFT JOIN FETCH tl.missions tlm " +
            "LEFT JOIN FETCH tlm.mission " +
            "WHERE tl.setType = 'TODOLIST' AND tl.todolistStatus = 'ACTIVE' AND tl.isActive = true")
    List<TodoList> findAllActiveTodoLists();
}
