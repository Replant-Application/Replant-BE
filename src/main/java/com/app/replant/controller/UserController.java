package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.controller.dto.UserResponse;
import com.app.replant.controller.dto.UserUpdateRequest;
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

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ReantService reantService;

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo(@AuthenticationPrincipal Long userId) {
        User user = userService.findById(userId);
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
        User user = userService.findById(userId);
        Reant reant = reantService.findByUserId(userId);
        return ApiResponse.success(UserProfileResponse.from(user, reant));
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
