package com.app.replant.service.member;

import com.app.replant.controller.dto.*;
import com.app.replant.entity.Member;
import com.app.replant.jwt.MemberDetail;
import com.app.replant.jwt.TokenProvider;
import com.app.replant.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 회원 서비스 구현
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    @Override
    @Transactional
    public Member join(JoinDto joinDto) {
        // ID 중복 체크
        if (memberRepository.existsByMemberId(joinDto.getId())) {
            throw new IllegalArgumentException("이미 사용 중인 ID입니다.");
        }

        // 비밀번호 암호화 및 회원 저장
        String encodedPassword = passwordEncoder.encode(joinDto.getPassword());
        Member member = joinDto.toEntity(encodedPassword);

        return memberRepository.save(member);
    }

    @Override
    @Transactional
    public LoginResponseDto login(LoginDto loginDto) {
        Member member = memberRepository.findByMemberId(loginDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (!passwordEncoder.matches(loginDto.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 로그인 시간 업데이트
        member.updateLastLoginAt();

        // JWT 토큰 생성
        MemberDetail memberDetail = new MemberDetail(member);
        TokenDto tokenDto = tokenProvider.generateTokenDto(memberDetail);

        return LoginResponseDto.builder()
                .accessToken(tokenDto.getAccessToken())
                .refreshToken(tokenDto.getRefreshToken())
                .name(member.getMemberName())
                .build();
    }

    @Override
    public Optional<Member> findByMemberId(String memberId) {
        return memberRepository.findByMemberId(memberId);
    }

    @Override
    public MemberDto getMemberInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        return MemberDto.fromEntity(member);
    }

    @Override
    @Transactional
    public void updateMember(Long memberId, MemberDto memberDto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 회원 정보 업데이트 로직 구현 필요
    }

    @Override
    @Transactional
    public void changePassword(Long memberId, ChangePasswordDto changePasswordDto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (!passwordEncoder.matches(changePasswordDto.getCurrentPassword(), member.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 비밀번호 변경 로직 구현 필요
    }

    @Override
    @Transactional
    public void withdrawMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 회원 탈퇴 로직 구현 필요
    }

    @Override
    public String findMemberId(String name, String phone) {
        Member member = memberRepository.findByMemberNameAndPhone(name, phone)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보가 없습니다."));

        return member.getMemberId();
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordDto resetPasswordDto) {
        Member member = memberRepository.findByMemberId(resetPasswordDto.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 비밀번호 재설정 로직 구현 필요
    }

    @Override
    public List<MemberResponseDto> getAllMembers() {
        // TODO: 실제 구현 필요
        return new java.util.ArrayList<>();
    }

    @Override
    public MemberResponseDto getMemberByIdForAdmin(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // TODO: MemberResponseDto 변환 로직 구현 필요
        return new MemberResponseDto();
    }

    @Override
    @Transactional
    public void updateEssentialCategories(Long memberId, List<com.app.replant.entity.type.CategoryType> categories) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // TODO: 필수 카테고리 업데이트 로직 구현 필요
    }
}
