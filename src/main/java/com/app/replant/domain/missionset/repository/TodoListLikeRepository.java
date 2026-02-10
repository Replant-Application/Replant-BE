package com.app.replant.domain.missionset.repository;

import com.app.replant.domain.missionset.entity.TodoList;
import com.app.replant.domain.missionset.entity.TodoListLike;
import com.app.replant.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TodoListLikeRepository extends JpaRepository<TodoListLike, Long> {

    long countByTodoList(TodoList todoList);

    boolean existsByTodoListAndUser(TodoList todoList, User user);

    Optional<TodoListLike> findByTodoListAndUser(TodoList todoList, User user);

    void deleteByTodoListAndUser(TodoList todoList, User user);

    void deleteByTodoList(TodoList todoList);
}
