package com.app.replant.domain.missionset.repository;

import com.app.replant.domain.missionset.entity.TodoList;
import com.app.replant.domain.missionset.entity.TodoListReview;
import com.app.replant.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TodoListReviewRepository extends JpaRepository<TodoListReview, Long> {

    /**
     * 투두리스트와 사용자로 리뷰 조회
     */
    Optional<TodoListReview> findByTodoListAndUser(TodoList todoList, User user);

    /**
     * 투두리스트와 사용자로 리뷰 존재 여부 확인
     */
    boolean existsByTodoListAndUser(TodoList todoList, User user);

    /**
     * 투두리스트의 리뷰 목록 조회 (최신순)
     */
    Page<TodoListReview> findByTodoListOrderByCreatedAtDesc(TodoList todoList, Pageable pageable);

    /**
     * 투두리스트의 모든 리뷰 조회 (평균 계산용)
     */
    java.util.List<TodoListReview> findByTodoList(TodoList todoList);

    /**
     * 투두리스트의 평균 별점 계산
     */
    @Query("SELECT AVG(r.rating) FROM TodoListReview r WHERE r.todoList = :todoList")
    Double calculateAverageRating(@Param("todoList") TodoList todoList);

    /**
     * 투두리스트의 리뷰 수 계산
     */
    long countByTodoList(TodoList todoList);

    /**
     * 사용자의 리뷰 목록 조회
     */
    Page<TodoListReview> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
