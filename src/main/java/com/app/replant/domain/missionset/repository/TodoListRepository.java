package com.app.replant.domain.missionset.repository;

import com.app.replant.domain.missionset.entity.TodoList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * TodoList Repository
 * QueryDSL을 사용한 복잡한 쿼리는 TodoListRepositoryCustom을 통해 구현
 */
@Repository
public interface TodoListRepository extends JpaRepository<TodoList, Long>, TodoListRepositoryCustom {
}
