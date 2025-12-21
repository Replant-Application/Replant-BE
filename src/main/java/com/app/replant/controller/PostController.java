package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.domain.post.dto.CommentRequest;
import com.app.replant.domain.post.dto.CommentResponse;
import com.app.replant.domain.post.dto.PostRequest;
import com.app.replant.domain.post.dto.PostResponse;
import com.app.replant.domain.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Post", description = "자유 게시판 API")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시글 목록 조회")
    @GetMapping
    public ApiResponse<Page<PostResponse>> getPosts(
            @RequestParam(required = false) Long missionId,
            @RequestParam(required = false) Long customMissionId,
            @RequestParam(required = false) Boolean badgeOnly,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PostResponse> posts = postService.getPosts(missionId, customMissionId, badgeOnly, pageable);
        return ApiResponse.success(posts);
    }

    @Operation(summary = "게시글 상세 조회")
    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> getPost(@PathVariable Long postId) {
        PostResponse post = postService.getPost(postId);
        return ApiResponse.success(post);
    }

    @Operation(summary = "게시글 작성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostResponse> createPost(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid PostRequest request) {
        PostResponse post = postService.createPost(userId, request);
        return ApiResponse.success(post);
    }

    @Operation(summary = "게시글 수정")
    @PutMapping("/{postId}")
    public ApiResponse<PostResponse> updatePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid PostRequest request) {
        PostResponse post = postService.updatePost(postId, userId, request);
        return ApiResponse.success(post);
    }

    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{postId}")
    public ApiResponse<Map<String, String>> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId) {
        postService.deletePost(postId, userId);

        Map<String, String> result = new HashMap<>();
        result.put("message", "게시글이 삭제되었습니다.");

        return ApiResponse.success(result);
    }

    // Comment endpoints
    @Operation(summary = "댓글 목록 조회")
    @GetMapping("/{postId}/comments")
    public ApiResponse<Page<CommentResponse>> getComments(
            @PathVariable Long postId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CommentResponse> comments = postService.getComments(postId, pageable);
        return ApiResponse.success(comments);
    }

    @Operation(summary = "댓글 작성")
    @PostMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> createComment(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid CommentRequest request) {
        CommentResponse comment = postService.createComment(postId, userId, request);
        return ApiResponse.success(comment);
    }

    @Operation(summary = "댓글 수정")
    @PutMapping("/{postId}/comments/{commentId}")
    public ApiResponse<CommentResponse> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid CommentRequest request) {
        CommentResponse comment = postService.updateComment(commentId, userId, request);
        return ApiResponse.success(comment);
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ApiResponse<Map<String, String>> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId) {
        postService.deleteComment(commentId, userId);

        Map<String, String> result = new HashMap<>();
        result.put("message", "댓글이 삭제되었습니다.");

        return ApiResponse.success(result);
    }
}
