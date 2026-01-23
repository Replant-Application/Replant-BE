package com.app.replant.domain.missionset.repository;

import com.app.replant.domain.missionset.entity.TodoList;
import com.app.replant.domain.missionset.enums.MissionSetType;
import com.app.replant.domain.missionset.enums.TodoListStatus;
import com.app.replant.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * TodoListRepository Custom Interface
 * QueryDSL을 사용한 복잡한 쿼리 메서드 정의
 */
public interface TodoListRepositoryCustom {

    Optional<TodoList> findByIdWithMissions(Long id);

    List<TodoList> findTodoListsByCreator(User creator, MissionSetType setType);

    List<TodoList> findTodoListsByCreatorAndStatus(User creator, TodoListStatus status);

    Page<TodoList> findTodoListsByCreator(User creator, Pageable pageable);

    long countActiveTodoListsByCreator(User creator);

    long countAllTodoListsByCreator(User creator);

    boolean existsByCreatorAndCreatedDate(User creator, LocalDateTime startOfDay, LocalDateTime endOfDay);

    Optional<TodoList> findTodoListByIdWithMissions(Long id);

    List<TodoList> findAllActiveTodoLists();

    Page<TodoList> findPublicTodoLists(Pageable pageable, String sortBy);
}
