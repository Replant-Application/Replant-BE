package com.app.replant.domain.missionset.repository;

import com.app.replant.domain.missionset.entity.TodoList;
import com.app.replant.domain.missionset.entity.TodoListLike;
import com.app.replant.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TodoListLikeRepository extends JpaRepository<TodoListLike, Long> {

    long countByTodoList(TodoList todoList);

    boolean existsByTodoListAndUser(TodoList todoList, User user);

    Optional<TodoListLike> findByTodoListAndUser(TodoList todoList, User user);

    void deleteByTodoListAndUser(TodoList todoList, User user);

    void deleteByTodoList(TodoList todoList);

    /** 목록용: 여러 투두리스트 ID에 대한 좋아요 수 (todoListId, count) */
    @Query("SELECT t.todoList.id, COUNT(t) FROM TodoListLike t WHERE t.todoList.id IN :ids GROUP BY t.todoList.id")
    List<Object[]> countGroupByTodoListId(@Param("ids") List<Long> ids);

    /** 목록용: 사용자가 좋아요한 투두리스트 ID 목록 (주어진 id 목록 내에서만) */
    @Query("SELECT t.todoList.id FROM TodoListLike t WHERE t.user.id = :userId AND t.todoList.id IN :ids")
    List<Long> findTodoListIdsLikedByUserAndTodoListIdIn(@Param("userId") Long userId, @Param("ids") List<Long> ids);
}
