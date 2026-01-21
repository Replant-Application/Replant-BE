package com.app.replant.domain.user.controller;


import com.app.replant.domain.user.dto.ChangePasswordDto;
import com.app.replant.domain.user.dto.EmailVerificationDto;
import com.app.replant.domain.user.dto.JoinDto;
import com.app.replant.domain.user.dto.LoginDto;
import com.app.replant.domain.user.dto.LoginResponseDto;
import com.app.replant.domain.user.dto.OAuthLoginRequest;
import com.app.replant.domain.user.dto.OAuthLoginResponse;
import com.app.replant.domain.user.dto.ResetPasswordDto;
import com.app.replant.domain.user.dto.TokenDto;
import com.app.replant.domain.user.dto.TokenRequestDto;
import com.app.replant.domain.user.dto.UserDto;
import com.app.replant.domain.user.dto.UserResponse;
import com.app.replant.domain.user.dto.UserUpdateRequest;

import com.app.replant.global.common.ApiResponse;

import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.enums.Gender;
import com.app.replant.domain.user.enums.MetropolitanArea;
import com.app.replant.domain.user.enums.OAuthProvider;
import com.app.replant.domain.user.security.UserDetail;
import com.app.replant.domain.user.service.OAuthService;
import com.app.replant.domain.user.service.UserService;
import com.app.replant.global.infrastructure.service.auth.AuthService;
import com.app.replant.global.infrastructure.service.mailService.MailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "인증", description = "회원가입, 로그인, 토큰 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final MailService mailService;
    private final OAuthService oAuthService;
    private final UserService userService;

    @Operation(summary = "회원가입", description = "새로운 회원을 등록하고 자동으로 로그인합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원가입 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 존재하는 이메일")
    @PostMapping("join")
    public ResponseEntity<ApiResponse<LoginResponseDto>> join(@RequestBody @Valid JoinDto joinDto) {
        LoginResponseDto result = authService.join(joinDto);
        return ResponseEntity.ok(ApiResponse.res(200, "회원가입에 성공했습니다", result));
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "비밀번호가 틀립니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "아이디가 존재하지 않습니다")
    @PostMapping("login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@RequestBody @Valid LoginDto loginDto) {
        LoginResponseDto result = authService.login(loginDto.getId(), loginDto.getPassword());
        return ResponseEntity.ok(ApiResponse.res(200, "SUCCESS", result));
    }

    @Operation(summary = "ID 중복 체크", description = "회원가입 전 이메일 중복 여부를 확인합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용 가능한 이메일")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일")
    @GetMapping("idCheck")
    public ResponseEntity<ApiResponse<Boolean>> idCheck(
            @Parameter(description = "중복 체크할 이메일", required = true)
            @RequestParam String memberId) {
        boolean isAvailable = authService.checkId(memberId);
        return ResponseEntity.ok(ApiResponse.res(200, "사용 가능한 이메일입니다", isAvailable));
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 새로운 Access Token을 발급받습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 재발급 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
    @PostMapping("refresh")
    public ResponseEntity<TokenDto> refresh(@RequestBody @Valid TokenRequestDto tokenRequestDto) {
        return ResponseEntity.ok(authService.refresh(tokenRequestDto));
    }

    @Operation(summary = "현재 로그인한 사용자 정보", description = "JWT 토큰으로 현재 로그인한 사용자의 정보를 조회합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    @GetMapping("user")
    public ResponseEntity<ApiResponse<UserDto>> getUserDetails(
            @Parameter(hidden = true) Authentication authentication) {
        UserDetail principal = (UserDetail) authentication.getPrincipal();
        User user = principal.getUser();

        UserDto userDto = UserDto.builder()
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .birthDate(user.getBirthDate() != null ? user.getBirthDate().toString() : null)
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .profileImg(user.getProfileImg())
                .role(user.getRole().name())
                .build();

        return ResponseEntity.ok(ApiResponse.res(200, "사용자의 정보를 불러왔습니다", userDto));
    }


    @Operation(summary = "이메일 인증번호 발송", description = "회원가입을 위한 인증번호를 이메일로 발송합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증번호 발송 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "이메일 발송 실패")
    @PostMapping("send-verification")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(
            @Parameter(description = "인증번호를 받을 이메일", required = true)
            @RequestBody @Valid EmailVerificationDto emailDto) {
        
        mailService.sendVerificationCode(emailDto.getEmail());
        return ResponseEntity.ok(ApiResponse.res(200, "인증번호가 발송되었습니다. 이메일을 확인해주세요."));
    }
    
    @Operation(summary = "이메일 인증번호 확인", description = "사용자가 입력한 인증번호를 검증합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "인증번호 불일치")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "408", description = "인증 시간 초과")
    @PostMapping("verify-email")
    public ResponseEntity<ApiResponse<Boolean>> verifyEmailCode(@RequestBody @Valid EmailVerificationDto emailDto) {
        
        boolean verified = mailService.verifyCode(emailDto.getEmail(), emailDto.getCode());
        return ResponseEntity.ok(ApiResponse.res(200, "이메일 인증이 완료되었습니다", verified));
    }
    
    @Operation(summary = "로그아웃", description = "AccessToken을 블랙리스트에 등록하고 Refresh Token을 삭제하여 로그아웃 처리합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공")
    @PostMapping("logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(hidden = true) Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        UserDetail principal = (UserDetail) authentication.getPrincipal();
        
        // Authorization 헤더에서 AccessToken 추출
        String accessToken = null;
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            accessToken = authorizationHeader.substring(7);
        }
        
        authService.logout(principal.getUsername(), accessToken);

        return ResponseEntity.ok(ApiResponse.res(200, "로그아웃되었습니다"));
    }
    
    @Operation(summary = "비밀번호 재설정 (임시 비밀번호 발급)", description = "이름과 이메일로 본인 확인 후 임시 비밀번호를 이메일로 발송합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "임시 비밀번호 발송 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 정보를 찾을 수 없습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "비활성화된 계정입니다")
    @PatchMapping("genPw")
    public ResponseEntity<ApiResponse<Void>> generateTemporaryPassword(
            @Parameter(description = "비밀번호 재설정 요청 정보 (이메일 + 이름)", required = true)
            @RequestBody @Valid ResetPasswordDto resetPasswordDto) {
        
        authService.resetPassword(resetPasswordDto);
        
        return ResponseEntity.ok(ApiResponse.res(200, "임시 비밀번호가 이메일로 발송되었습니다. 로그인 후 비밀번호를 변경해주세요."));
    }
    
    @Operation(summary = "비밀번호 변경", description = "기존 비밀번호를 확인하고 새 비밀번호로 변경합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "비밀번호 변경 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "data": null,
                              "message": "비밀번호가 성공적으로 변경되었습니다."
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "기존 비밀번호가 일치하지 않습니다",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "ACCOUNT-011",
                                "message": "비밀번호가 틀립니다."
                              }
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "회원 정보를 찾을 수 없습니다",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "ACCOUNT-010",
                                "message": "아이디가 존재하지 않습니다."
                              }
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "새 비밀번호가 기존 비밀번호와 동일합니다",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "error": {
                                "code": "ACCOUNT-020",
                                "message": "새 비밀번호가 기존 비밀번호와 동일합니다."
                              }
                            }
                            """))
            )
    })
    @PatchMapping("resetPw")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Parameter(description = "비밀번호 변경 요청 정보 (이메일 + 기존 비밀번호 + 새 비밀번호)", required = true)
            @RequestBody @Valid ChangePasswordDto changePasswordDto) {

        authService.changePassword(changePasswordDto);

        return ResponseEntity.ok(ApiResponse.res(200, "비밀번호가 성공적으로 변경되었습니다."));
    }

    @Operation(summary = "OAuth 로그인/회원가입", description = "OAuth 제공자를 통한 소셜 로그인 및 자동 회원가입")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "OAuth 제공자 오류")
    @PostMapping("oauth/{provider}")
    public ResponseEntity<ApiResponse<OAuthLoginResponse>> oauthLogin(
            @Parameter(description = "OAuth 제공자 (KAKAO, GOOGLE, NAVER, APPLE)", required = true)
            @PathVariable("provider") String providerStr,
            @Parameter(description = "OAuth Access Token", required = true)
            @RequestBody @Valid OAuthLoginRequest request) {

        OAuthProvider provider = OAuthProvider.valueOf(providerStr.toUpperCase());
        OAuthLoginResponse response = oAuthService.login(provider, request.getAccessToken());

        String message = response.isNewUser() ? "회원가입이 완료되었습니다." : "로그인에 성공했습니다.";
        return ResponseEntity.ok(ApiResponse.res(200, message, response));
    }

    @Operation(summary = "OAuth 추가정보 입력", description = "OAuth 로그인 후 추가정보(닉네임, 생년월일, 성별 등)를 입력합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추가정보 입력 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    @PutMapping("oauth/{provider}")
    public ResponseEntity<ApiResponse<UserResponse>> updateOAuthAdditionalInfo(
            @Parameter(description = "OAuth 제공자 (KAKAO, GOOGLE, NAVER, APPLE)", required = true)
            @PathVariable("provider") String providerStr,
            @Parameter(hidden = true) Authentication authentication,
            @RequestBody @Valid UserUpdateRequest request) {

        UserDetail principal = (UserDetail) authentication.getPrincipal();
        User user = userService.updateUser(principal.getUser().getId(), request);

        return ResponseEntity.ok(ApiResponse.res(200, "추가정보가 저장되었습니다.", UserResponse.from(user)));
    }

    @Operation(summary = "광역자치단체 목록 조회", description = "회원가입 시 선택 가능한 지역(광역자치단체) 목록을 조회합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("regions")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getRegions() {
        List<Map<String, String>> regions = Arrays.stream(MetropolitanArea.values())
                .map(area -> Map.of(
                        "code", area.name(),
                        "name", area.getDisplayName()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.res(200, "지역 목록을 조회했습니다", regions));
    }

    @Operation(summary = "성별 목록 조회", description = "회원가입 시 선택 가능한 성별 목록을 조회합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("genders")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getGenders() {
        List<Map<String, String>> genders = Arrays.stream(Gender.values())
                .map(gender -> Map.of(
                        "code", gender.name(),
                        "name", gender == Gender.MALE ? "남성" : "여성"
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.res(200, "성별 목록을 조회했습니다", genders));
    }

}
