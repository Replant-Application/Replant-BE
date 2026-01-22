package com.app.replant.domain.missionset.repository;

import com.app.replant.domain.missionset.entity.TodoList;
import com.app.replant.domain.missionset.enums.MissionSetType;
import com.app.replant.domain.missionset.enums.TodoListStatus;
import com.app.replant.domain.user.entity.User;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.app.replant.domain.missionset.entity.QTodoList.todoList;
import static com.app.replant.domain.missionset.entity.QTodoListMission.todoListMission;
import static com.app.replant.domain.mission.entity.QMission.mission;
import static com.app.replant.domain.user.entity.QUser.user;

/**
 * TodoListRepository Custom Implementation
 * QueryDSL을 사용한 복잡한 쿼리 구현
 */
@RequiredArgsConstructor
public class TodoListRepositoryCustomImpl implements TodoListRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // ========================================
    // 공통 조건
    // ========================================

    private com.querydsl.core.types.dsl.BooleanExpression isActive() {
        return todoList.isActive.isTrue();
    }

    @Override
    public Optional<TodoList> findByIdWithMissions(Long id) {
        TodoList result = queryFactory
                .selectFrom(todoList)
                .leftJoin(todoList.missions, todoListMission).fetchJoin()
                .leftJoin(todoListMission.mission, mission).fetchJoin()
                .where(todoList.id.eq(id)
                        .and(isActive()))
                .distinct()
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<TodoList> findTodoListsByCreator(User creator, MissionSetType setType) {
        return queryFactory
                .selectFrom(todoList)
                .where(todoList.creator.eq(creator)
                        .and(todoList.setType.eq(setType))
                        .and(isActive()))
                .orderBy(todoList.createdAt.desc())
                .fetch();
    }

    @Override
    public List<TodoList> findTodoListsByCreatorAndStatus(User creator, TodoListStatus status) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(todoList.creator.eq(creator));
        builder.and(todoList.setType.eq(MissionSetType.TODOLIST)
                .or(todoList.setType.eq(MissionSetType.SHARED)));
        builder.and(todoList.todolistStatus.eq(status)
                .or(todoList.todolistStatus.isNull()));
        builder.and(isActive());

        return queryFactory
                .selectFrom(todoList)
                .where(builder)
                .orderBy(todoList.createdAt.desc())
                .fetch();
    }

    @Override
    public Page<TodoList> findTodoListsByCreator(User creator, Pageable pageable) {
        JPAQuery<TodoList> query = queryFactory
                .selectFrom(todoList)
                .where(todoList.creator.eq(creator)
                        .and(todoList.setType.eq(MissionSetType.TODOLIST))
                        .and(isActive()))
                .orderBy(todoList.createdAt.desc());

        return getPage(query, pageable);
    }

    @Override
    public long countActiveTodoListsByCreator(User creator) {
        Long count = queryFactory
                .select(todoList.count())
                .from(todoList)
                .where(todoList.creator.eq(creator)
                        .and(todoList.setType.eq(MissionSetType.TODOLIST))
                        .and(todoList.todolistStatus.eq(TodoListStatus.ACTIVE))
                        .and(isActive()))
                .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public long countAllTodoListsByCreator(User creator) {
        Long count = queryFactory
                .select(todoList.count())
                .from(todoList)
                .where(todoList.creator.eq(creator)
                        .and(todoList.setType.eq(MissionSetType.TODOLIST))
                        .and(isActive()))
                .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public boolean existsByCreatorAndCreatedDate(User creator, LocalDateTime startOfDay, LocalDateTime endOfDay) {
        Long count = queryFactory
                .select(todoList.count())
                .from(todoList)
                .where(todoList.creator.eq(creator)
                        .and(todoList.setType.eq(MissionSetType.TODOLIST))
                        .and(isActive())
                        .and(todoList.createdAt.goe(startOfDay))
                        .and(todoList.createdAt.lt(endOfDay)))
                .fetchOne();

        return count != null && count > 0;
    }

    @Override
    public Optional<TodoList> findTodoListByIdWithMissions(Long id) {
        TodoList result = queryFactory
                .selectFrom(todoList)
                .leftJoin(todoList.missions, todoListMission).fetchJoin()
                .leftJoin(todoListMission.mission, mission).fetchJoin()
                .where(todoList.id.eq(id)
                        .and(todoList.setType.eq(MissionSetType.TODOLIST))
                        .and(isActive()))
                .distinct()
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<TodoList> findAllActiveTodoLists() {
        return queryFactory
                .selectFrom(todoList)
                .leftJoin(todoList.missions, todoListMission).fetchJoin()
                .leftJoin(todoListMission.mission, mission).fetchJoin()
                .where(todoList.setType.eq(MissionSetType.TODOLIST)
                        .and(todoList.todolistStatus.eq(TodoListStatus.ACTIVE))
                        .and(isActive()))
                .distinct()
                .fetch();
    }

    @Override
    public Page<TodoList> findPublicTodoLists(Pageable pageable, String sortBy) {
        JPAQuery<TodoList> query = queryFactory
                .selectFrom(todoList)
                .leftJoin(todoList.creator, user).fetchJoin()
                .where(todoList.setType.eq(MissionSetType.TODOLIST)
                        .and(isActive()))
                .distinct();

        // 정렬 기준에 따라 정렬
        if ("popular".equalsIgnoreCase(sortBy)) {
            // 인기순: 완료 수가 많은 순, 그 다음 생성일 최신순
            query.orderBy(todoList.completedCount.desc(),
                    todoList.createdAt.desc());
        } else {
            // 최신순: 생성일 최신순
            query.orderBy(todoList.createdAt.desc());
        }

        return getPage(query, pageable);
    }

    // ========================================
    // 헬퍼 메서드
    // ========================================

    /**
     * QueryDSL 쿼리를 Page로 변환
     */
    private Page<TodoList> getPage(JPAQuery<TodoList> query, Pageable pageable) {
        // 페이징 적용
        JPAQuery<TodoList> pagedQuery = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        // Count 쿼리 - JOIN 없이 where 조건만 복사
        com.querydsl.core.types.Predicate whereCondition = query.getMetadata().getWhere();
        JPAQuery<Long> countQuery = queryFactory
                .select(todoList.count())
                .from(todoList)
                .where(whereCondition);

        return PageableExecutionUtils.getPage(
                pagedQuery.fetch(),
                pageable,
                () -> {
                    Long count = countQuery.fetchOne();
                    return count != null ? count : 0L;
                }
        );
    }
}
