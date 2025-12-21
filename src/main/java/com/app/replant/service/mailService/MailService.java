package com.app.replant.service.mailService;

/**
 * 메일 서비스 인터페이스
 */
public interface MailService {

    /**
     * 이메일 인증 코드 발송
     */
    String sendVerificationCode(String email);

    /**
     * 인증 코드 검증
     */
    boolean verifyCode(String email, String code);

    /**
     * 비밀번호 재설정 메일 발송
     */
    void sendPasswordResetEmail(String email, String resetToken);

    /**
     * 임시 비밀번호 발송
     */
    String sendTemporaryPassword(String email, String name);
}
