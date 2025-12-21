package com.app.replant.service.mailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 메일 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${GOOGLE_MAIL:noreply@replant.com}")
    private String fromEmail;

    // 인증 코드 임시 저장소 (실제로는 Redis 사용 권장)
    private final Map<String, String> verificationCodes = new ConcurrentHashMap<>();

    @Override
    public String sendVerificationCode(String email) {
        try {
            // 6자리 인증 코드 생성
            String code = String.format("%06d", new Random().nextInt(1000000));

            // 메일 발송
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("[Replant] 이메일 인증 코드");
            message.setText("인증 코드: " + code + "\n\n이 코드는 5분간 유효합니다.");

            mailSender.send(message);

            // 인증 코드 저장 (5분 유효)
            verificationCodes.put(email, code);

            log.info("Verification code sent to: {}", email);
            return code;
        } catch (Exception e) {
            log.error("Failed to send verification email", e);
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }
    }

    @Override
    public boolean verifyCode(String email, String code) {
        String savedCode = verificationCodes.get(email);
        if (savedCode != null && savedCode.equals(code)) {
            verificationCodes.remove(email);
            return true;
        }
        return false;
    }

    @Override
    public void sendPasswordResetEmail(String email, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("[Replant] 비밀번호 재설정");
            message.setText("비밀번호 재설정 링크: " + resetToken + "\n\n이 링크는 1시간 동안 유효합니다.");

            mailSender.send(message);

            log.info("Password reset email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email", e);
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }
    }

    @Override
    public String sendTemporaryPassword(String email, String name) {
        try {
            // 임시 비밀번호 생성 (8자리 랜덤)
            String temporaryPassword = String.format("%08d", new Random().nextInt(100000000));

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("[Replant] 임시 비밀번호");
            message.setText("안녕하세요 " + name + "님,\n\n" +
                    "임시 비밀번호: " + temporaryPassword + "\n\n" +
                    "로그인 후 반드시 비밀번호를 변경해주세요.");

            mailSender.send(message);

            log.info("Temporary password sent to: {}", email);
            return temporaryPassword;
        } catch (Exception e) {
            log.error("Failed to send temporary password email", e);
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }
    }
}
