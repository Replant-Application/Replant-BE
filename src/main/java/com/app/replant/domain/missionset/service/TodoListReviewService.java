package com.app.replant.domain.missionset.service;

import com.app.replant.domain.missionset.dto.TodoListReviewDto;
import com.app.replant.domain.missionset.entity.TodoList;
import com.app.replant.domain.missionset.entity.TodoListReview;
import com.app.replant.domain.missionset.repository.TodoListRepository;
import com.app.replant.domain.missionset.repository.TodoListReviewRepository;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.app.replant.global.exception.CustomException;
import com.app.replant.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TodoListReviewService {

    private final TodoListReviewRepository reviewRepository;
    private final TodoListRepository todoListRepository;
    private final UserRepository userRepository;

    /**
     * 리뷰 작성
     */
    @Transactional
    public TodoListReviewDto.Response createReview(Long todoListId, Long userId, TodoListReviewDto.CreateRequest request) {
        TodoList todoList = todoListRepository.findById(todoListId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 자신의 투두리스트에는 리뷰 불가
        if (todoList.isCreator(userId)) {
            throw new CustomException(ErrorCode.CANNOT_REVIEW_OWN_SET);
        }

        // 이미 리뷰를 작성한 경우
        if (reviewRepository.existsByTodoListAndUser(todoList, user)) {
            throw new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        TodoListReview review = TodoListReview.builder()
                .todoList(todoList)
                .user(user)
                .rating(request.getRating())
                .content(request.getContent())
                .build();

        reviewRepository.save(review);

        // 투두리스트 평균 별점 업데이트
        updateTodoListRating(todoList);

        log.info("투두리스트 리뷰 작성: todoListId={}, userId={}, rating={}", todoListId, userId, request.getRating());
        return TodoListReviewDto.Response.from(review);
    }

    /**
     * 리뷰 수정
     */
    @Transactional
    public TodoListReviewDto.Response updateReview(Long reviewId, Long userId, TodoListReviewDto.UpdateRequest request) {
        TodoListReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        // 본인 리뷰만 수정 가능
        if (!review.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        review.update(request.getRating(), request.getContent());

        // 투두리스트 평균 별점 업데이트
        updateTodoListRating(review.getTodoList());

        log.info("투두리스트 리뷰 수정: reviewId={}, userId={}", reviewId, userId);
        return TodoListReviewDto.Response.from(review);
    }

    /**
     * 리뷰 삭제
     */
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        TodoListReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        // 본인 리뷰만 삭제 가능
        if (!review.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        TodoList todoList = review.getTodoList();
        reviewRepository.delete(review);

        // 투두리스트 평균 별점 업데이트
        updateTodoListRating(todoList);

        log.info("투두리스트 리뷰 삭제: reviewId={}, userId={}", reviewId, userId);
    }

    /**
     * 투두리스트의 리뷰 목록 조회
     */
    public Page<TodoListReviewDto.Response> getReviews(Long todoListId, Pageable pageable) {
        TodoList todoList = todoListRepository.findById(todoListId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

        return reviewRepository.findByTodoListOrderByCreatedAtDesc(todoList, pageable)
                .map(TodoListReviewDto.Response::from);
    }

    /**
     * 내가 작성한 리뷰 조회
     */
    public TodoListReviewDto.Response getMyReview(Long todoListId, Long userId) {
        TodoList todoList = todoListRepository.findById(todoListId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_SET_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return reviewRepository.findByTodoListAndUser(todoList, user)
                .map(TodoListReviewDto.Response::from)
                .orElse(null);
    }

    /**
     * 투두리스트 평균 별점 업데이트
     */
    private void updateTodoListRating(TodoList todoList) {
        // 리뷰 평점 업데이트 제거됨 (공유 기능 제거)
    }
}
