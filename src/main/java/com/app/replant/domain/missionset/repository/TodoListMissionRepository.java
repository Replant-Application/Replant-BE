package com.app.replant.domain.missionset.repository;

import com.app.replant.domain.mission.entity.Mission;
import com.app.replant.domain.missionset.entity.TodoList;
import com.app.replant.domain.missionset.entity.TodoListMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TodoListMissionRepository extends JpaRepository<TodoListMission, Long> {

    // 투두리스트에 포함된 미션 목록
    List<TodoListMission> findByTodoListOrderByDisplayOrderAsc(TodoList todoList);

    // 특정 투두리스트에 특정 미션이 있는지 확인
    boolean existsByTodoListAndMission(TodoList todoList, Mission mission);

    // 특정 투두리스트에서 특정 미션 찾기
    Optional<TodoListMission> findByTodoListAndMission(TodoList todoList, Mission mission);

    // 투두리스트의 최대 displayOrder 조회
    @Query("SELECT COALESCE(MAX(msm.displayOrder), 0) FROM TodoListMission msm WHERE msm.todoList = :todoList")
    Integer findMaxDisplayOrderByTodoList(@Param("todoList") TodoList todoList);

    // 투두리스트의 미션 수
    long countByTodoList(TodoList todoList);
}
