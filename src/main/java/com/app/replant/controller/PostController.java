package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.domain.post.dto.CommentRequest;
import com.app.replant.domain.post.dto.CommentResponse;
import com.app.replant.domain.post.dto.PostRequest;
import com.app.replant.domain.post.dto.PostResponse;
import com.app.replant.domain.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

@Tag(name = "Community Post", description = "커뮤니티 자유 게시판 API")
@RestController
@RequestMapping("/api/community/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시글 목록 조회",
            description = "자유 게시판 게시글 목록을 조회합니다. 미션 ID나 뱃지 소유 여부로 필터링 가능합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "data": {
                                "content": [
                                  {
                                    "id": 1,
                                    "userId": 10,
                                    "userNickname": "사용자닉네임",
                                    "userProfileImg": "https://...",
                                    "title": "미션 완료 후기",
                                    "content": "이 미션 정말 좋았어요!",
                                    "imageUrls": ["https://..."],
                                    "commentCount": 5,
                                    "createdAt": "2024-01-15T10:30:00"
                                  }
                                ],
                                "totalElements": 1,
                                "totalPages": 1,
                                "number": 0
                              }
                            }
                            """)))
    })
    @GetMapping
    public ApiResponse<Page<PostResponse>> getPosts(
            @Parameter(description = "시스템 미션 ID로 필터링")
            @RequestParam(required = false) Long missionId,
            @Parameter(description = "커스텀 미션 ID로 필터링")
            @RequestParam(required = false) Long customMissionId,
            @Parameter(description = "뱃지 소유자 게시글만 조회")
            @RequestParam(required = false) Boolean badgeOnly,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PostResponse> posts = postService.getPosts(missionId, customMissionId, badgeOnly, pageable);
        return ApiResponse.success(posts);
    }

    @Operation(summary = "게시글 상세 조회")
    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> getPost(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId) {
        PostResponse post = postService.getPost(postId);
        return ApiResponse.success(post);
    }

    @Operation(summary = "게시글 작성",
            description = "새 게시글을 작성합니다. 로그인 필요.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "title": "미션 완료 후기입니다",
                      "content": "이 미션을 완료하고 나니 정말 뿌듯하네요!",
                      "imageUrls": ["https://example.com/image1.jpg"],
                      "missionId": 10
                    }
                    """))
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostResponse> createPost(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid PostRequest request) {
        PostResponse post = postService.createPost(userId, request);
        return ApiResponse.success(post);
    }

    @Operation(summary = "게시글 수정",
            description = "자신이 작성한 게시글을 수정합니다.")
    @PutMapping("/{postId}")
    public ApiResponse<PostResponse> updatePost(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid PostRequest request) {
        PostResponse post = postService.updatePost(postId, userId, request);
        return ApiResponse.success(post);
    }

    @Operation(summary = "게시글 삭제",
            description = "자신이 작성한 게시글을 삭제합니다.")
    @DeleteMapping("/{postId}")
    public ApiResponse<Map<String, String>> deletePost(
            @Parameter(description = "게시글 ID", example = "1")
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
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CommentResponse> comments = postService.getComments(postId, pageable);
        return ApiResponse.success(comments);
    }

    @Operation(summary = "댓글 작성")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "content": "좋은 글이네요!",
                      "parentId": null
                    }
                    """))
    )
    @PostMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> createComment(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid CommentRequest request) {
        CommentResponse comment = postService.createComment(postId, userId, request);
        return ApiResponse.success(comment);
    }

    @Operation(summary = "댓글 수정")
    @PutMapping("/{postId}/comments/{commentId}")
    public ApiResponse<CommentResponse> updateComment(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @Parameter(description = "댓글 ID", example = "1")
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid CommentRequest request) {
        CommentResponse comment = postService.updateComment(commentId, userId, request);
        return ApiResponse.success(comment);
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ApiResponse<Map<String, String>> deleteComment(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @Parameter(description = "댓글 ID", example = "1")
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId) {
        postService.deleteComment(commentId, userId);

        Map<String, String> result = new HashMap<>();
        result.put("message", "댓글이 삭제되었습니다.");

        return ApiResponse.success(result);
    }

    // Like endpoints
    @Operation(summary = "좋아요 토글",
            description = "게시글 좋아요를 추가하거나 취소합니다. 이미 좋아요한 경우 취소, 아닌 경우 추가됩니다.")
    @PostMapping("/{postId}/like")
    public ApiResponse<Map<String, Object>> toggleLike(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId) {
        Map<String, Object> result = postService.toggleLike(postId, userId);
        return ApiResponse.success(result);
    }
}
