package com.app.replant.domain.user.controller;

import com.app.replant.global.common.ApiResponse;
import com.app.replant.domain.user.dto.RestoreAccountRequest;
import com.app.replant.domain.user.dto.UserResponse;
import com.app.replant.domain.user.dto.UserStatsResponse;
import com.app.replant.domain.user.dto.UserUpdateRequest;
import com.app.replant.domain.reant.entity.Reant;
import com.app.replant.domain.reant.service.ReantService;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class UserController {

    private final UserService userService;
    private final ReantService reantService;

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo(@AuthenticationPrincipal Long userId) {
        log.info("=== API 호출: GET /api/users/me, userId={} ===", userId);
        User user = userService.findById(userId);
        log.info("User 조회 완료: userId={}, reantLoaded={}", user.getId(), user.getReant() != null);
        return ApiResponse.success(UserResponse.from(user));
    }

    @Operation(summary = "내 정보 수정")
    @PutMapping("/me")
    public ApiResponse<UserResponse> updateMyInfo(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid UserUpdateRequest request) {
        User user = userService.updateUser(userId, request);
        return ApiResponse.success(UserResponse.from(user));
    }

    @Operation(summary = "다른 유저 프로필 조회")
    @GetMapping("/{userId}")
    public ApiResponse<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        User user = userService.findById(userId);  // 이미 reant를 포함해서 조회됨
        Reant reant = user.getReant();  // 중복 조회 방지 - 이미 로드된 reant 사용
        return ApiResponse.success(UserProfileResponse.from(user, reant));
    }

    @Operation(summary = "회원 탈퇴", description = "회원 탈퇴를 처리합니다. Soft Delete 방식으로 처리되며, 개인정보는 마스킹됩니다.")
    @DeleteMapping("/me")
    public ApiResponse<Map<String, String>> deleteMyAccount(@AuthenticationPrincipal Long userId) {
        userService.deleteUser(userId);
        
        Map<String, String> result = new HashMap<>();
        result.put("message", "회원 탈퇴가 완료되었습니다.");
        return ApiResponse.success(result);
    }

    @Operation(summary = "계정 복구", description = "탈퇴한 계정을 이메일과 비밀번호로 복구합니다. 탈퇴 후 30일 이내에만 복구 가능합니다.")
    @PostMapping("/restore")
    public ApiResponse<Map<String, String>> restoreAccount(@RequestBody @Valid RestoreAccountRequest request) {
        userService.restoreUserByEmail(request.getEmail(), request.getPassword());
        
        Map<String, String> result = new HashMap<>();
        result.put("message", "계정이 복구되었습니다.");
        return ApiResponse.success(result);
    }

    @Operation(summary = "마이 페이지 통계 조회", 
               description = "완료 미션 수, 게시글 수 등 통계 정보를 한 번에 조회합니다. " +
                           "대량 조회를 방지하기 위해 통계만 필요한 경우 이 API를 사용하세요.")
    @GetMapping("/me/stats")
    public ApiResponse<UserStatsResponse> getMyStats(@AuthenticationPrincipal Long userId) {
        log.info("=== API 호출: GET /api/users/me/stats, userId={} ===", userId);
        UserStatsResponse stats = userService.getUserStats(userId);
        log.info("통계 조회 완료: userId={}, completedMissions={}, posts={}, diaries={}, badges={}", 
                userId, stats.getCompletedMissionsCount(), stats.getPostsCount(), 
                stats.getDiariesCount(), stats.getBadgesCount());
        return ApiResponse.success(stats);
    }

    @Getter
    public static class UserProfileResponse {
        private Long id;
        private String nickname;
        private String profileImg;
        private ReantInfo reant;

        @Getter
        public static class ReantInfo {
            private String name;
            private Integer level;
            private String stage;

            public static ReantInfo from(Reant reant) {
                ReantInfo info = new ReantInfo();
                info.name = reant.getName();
                info.level = reant.getLevel();
                info.stage = reant.getStage().name();
                return info;
            }
        }

        public static UserProfileResponse from(User user, Reant reant) {
            UserProfileResponse response = new UserProfileResponse();
            response.id = user.getId();
            response.nickname = user.getNickname();
            response.profileImg = user.getProfileImg();
            if (reant != null) {
                response.reant = ReantInfo.from(reant);
            }
            return response;
        }
    }
}
