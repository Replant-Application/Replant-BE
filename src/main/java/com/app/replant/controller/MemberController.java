package com.app.replant.controller;


import com.app.replant.common.ApiResponse;
import com.app.replant.controller.dto.UpdateEssentialCategoriesRequest;
import com.app.replant.jwt.MemberDetail;
import com.app.replant.service.member.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@Tag(name = "회원", description = "회원 및 개인 설정 관련 API")
@RestController
@RequestMapping("/member/*")
@RequiredArgsConstructor
@Slf4j
public class MemberController {
    private final MemberService memberService;

    @Operation(summary = "필수 카테고리 설정", description = "회원의 필수 카테고리를 설정하고 해당 카테고리 결제내역을 총지출에서 미포함 처리합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "설정 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (JWT 필요)")
    @PostMapping("essential-categories")
    public ResponseEntity<ApiResponse<Void>> updateEssentialCategories(
            @Parameter(hidden = true) Authentication authentication,
            @RequestBody @Valid UpdateEssentialCategoriesRequest request
    ) {
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getId();

        memberService.updateEssentialCategories(memberId, request.getEssentialCategories());

        return ResponseEntity.ok(
                ApiResponse.res(200, "필수 카테고리가 설정되었으며, 해당 카테고리의 결제내역은 총지출에서 제외되었습니다.")
        );
    }
}
