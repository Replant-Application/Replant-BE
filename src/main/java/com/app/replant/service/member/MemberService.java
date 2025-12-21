package com.app.replant.service.member;

import com.app.replant.controller.dto.*;
import com.app.replant.entity.Member;

import java.util.List;
import java.util.Optional;

/**
 * 회원 서비스 인터페이스
 */
public interface MemberService {

    /**
     * 회원가입
     */
    Member join(JoinDto joinDto);

    /**
     * 로그인
     */
    LoginResponseDto login(LoginDto loginDto);

    /**
     * 회원 ID로 조회
     */
    Optional<Member> findByMemberId(String memberId);

    /**
     * 회원 정보 조회
     */
    MemberDto getMemberInfo(Long memberId);

    /**
     * 회원 정보 수정
     */
    void updateMember(Long memberId, MemberDto memberDto);

    /**
     * 비밀번호 변경
     */
    void changePassword(Long memberId, ChangePasswordDto changePasswordDto);

    /**
     * 회원 탈퇴
     */
    void withdrawMember(Long memberId);

    /**
     * ID 찾기
     */
    String findMemberId(String name, String phone);

    /**
     * 비밀번호 재설정
     */
    void resetPassword(ResetPasswordDto resetPasswordDto);

    /**
     * 모든 회원 조회 (관리자용)
     */
    List<MemberResponseDto> getAllMembers();

    /**
     * 회원 상세 조회 (관리자용)
     */
    MemberResponseDto getMemberByIdForAdmin(Long memberId);

    /**
     * 필수 카테고리 업데이트
     */
    void updateEssentialCategories(Long memberId, List<com.app.replant.entity.type.CategoryType> categories);
}
