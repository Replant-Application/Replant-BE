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

    // ============ 공유 관련 쿼리 제거됨 ============

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
    // TODOLIST 타입이거나, SHARED 타입인 경우 모두 조회 (기존 데이터 호환)
    // ACTIVE 상태이거나 todolistStatus가 NULL인 경우 조회
    @Query("SELECT tl FROM TodoList tl WHERE tl.creator = :creator " +
            "AND (tl.setType = 'TODOLIST' OR tl.setType = 'SHARED') " +
            "AND (tl.todolistStatus = :status OR tl.todolistStatus IS NULL) " +
            "AND tl.isActive = true ORDER BY tl.createdAt DESC")
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

    // 당일 생성된 투두리스트 존재 여부 확인
    @Query("SELECT COUNT(tl) > 0 FROM TodoList tl WHERE tl.creator = :creator AND tl.setType = 'TODOLIST' " +
            "AND tl.isActive = true AND tl.createdAt >= :startOfDay AND tl.createdAt < :endOfDay")
    boolean existsByCreatorAndCreatedDate(@Param("creator") User creator, 
                                          @Param("startOfDay") java.time.LocalDateTime startOfDay,
                                          @Param("endOfDay") java.time.LocalDateTime endOfDay);

    // 투두리스트 상세 조회 (미션 목록 포함)
    @Query("SELECT DISTINCT tl FROM TodoList tl " +
            "LEFT JOIN FETCH tl.missions tlm " +
            "LEFT JOIN FETCH tlm.mission " +
            "WHERE tl.id = :id AND tl.setType = 'TODOLIST' AND tl.isActive = true")
    Optional<TodoList> findTodoListByIdWithMissions(@Param("id") Long id);

    // ============ 공유 관련 쿼리 제거됨 ============
    // findPrivateTodoListsByCreator, findByIdForShare, findPrivateTodoListsByCreatorV2

    /**
     * 모든 활성 투두리스트 조회 (만료 처리용)
     */
    @Query("SELECT DISTINCT tl FROM TodoList tl " +
            "LEFT JOIN FETCH tl.missions tlm " +
            "LEFT JOIN FETCH tlm.mission " +
            "WHERE tl.setType = 'TODOLIST' AND tl.todolistStatus = 'ACTIVE' AND tl.isActive = true")
    List<TodoList> findAllActiveTodoLists();
}
