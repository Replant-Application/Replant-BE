package com.app.replant.global.infrastructure.service.mailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 메일 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    @Value("${GOOGLE_MAIL:noreply@replant.com}")
    private String fromEmail;

    @Value("${REPLANT_URL:https://replant.com}")
    private String replantUrl;

    @Override
    public String sendVerificationCode(String email) {
        try {
            // 6자리 인증 코드 생성
            String code = String.format("%06d", new Random().nextInt(1000000));

            // HTML 이메일 생성
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("[Replant] 이메일 인증 코드");
            helper.setText(createEmailTemplate(
                    "이메일 인증 코드",
                    "Replant 회원가입을 위한 이메일 인증 코드입니다.<br>아래 인증 코드를 입력해주세요.",
                    "인증 코드",
                    code,
                    "코드를 복사하여 입력창에 붙여넣으세요.",
                    new String[]{
                            "이 코드는 5분간 유효합니다.",
                            "본인이 요청하지 않은 경우 이 메일을 무시하셔도 됩니다."
                    }
            ), true);

            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", email);

            // Redis에 인증 코드 저장 (5분 유효) - 실패해도 이메일은 이미 발송됨
            try {
                String redisKey = "email:verification:" + email;
                redisTemplate.opsForValue().set(redisKey, code, 5, TimeUnit.MINUTES);
                log.info("Verification code saved to Redis for: {}", email);
            } catch (Exception redisException) {
                log.warn("Redis에 인증 코드 저장 실패 (이메일은 이미 발송됨): {}. Error: {}", 
                        email, redisException.getMessage());
                // Redis 실패해도 이메일은 발송되었으므로 계속 진행
            }

            return code;
        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}. Error: {}", email, e.getMessage(), e);
            throw new RuntimeException("이메일 발송에 실패했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}. Error: {}", email, e.getMessage(), e);
            throw new RuntimeException("이메일 발송에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyCode(String email, String code) {
        try {
            String redisKey = "email:verification:" + email;
            String savedCode = redisTemplate.opsForValue().get(redisKey);
            if (savedCode != null && savedCode.equals(code)) {
                redisTemplate.delete(redisKey);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Redis 연결 실패로 인증 코드 검증 불가: email={}, error={}", email, e.getMessage(), e);
            throw new RuntimeException("인증 코드 검증에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    @Override
    public void sendPasswordResetEmail(String email, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("[Replant] 비밀번호 재설정");
            String resetUrl = replantUrl + "/reset-password?token=" + resetToken;
            helper.setText(createEmailTemplate(
                    "비밀번호 재설정",
                    "비밀번호 재설정을 요청하셨습니다.<br>아래 링크를 클릭하여 비밀번호를 재설정해주세요.<br><br>" +
                    "<a href=\"" + resetUrl + "\" style=\"display: inline-block; background-color: #3A4D39; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 6px; font-size: 16px; font-weight: 600;\">비밀번호 재설정하기</a>",
                    "재설정 링크",
                    resetUrl,
                    "링크를 복사하여 브라우저에 붙여넣으세요.",
                    new String[]{
                            "이 링크는 1시간 동안 유효합니다.",
                            "본인이 요청하지 않은 경우 이 메일을 무시하셔도 됩니다."
                    }
            ), true);

            mailSender.send(message);

            log.info("Password reset email sent to: {}", email);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}. Error: {}", email, e.getMessage(), e);
            throw new RuntimeException("이메일 발송에 실패했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}. Error: {}", email, e.getMessage(), e);
            throw new RuntimeException("이메일 발송에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public String sendTemporaryPassword(String email, String name) {
        try {
            // 임시 비밀번호 생성 (8자리 랜덤)
            String temporaryPassword = String.format("%08d", new Random().nextInt(100000000));

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("[Replant] 임시 비밀번호");
            helper.setText(createEmailTemplate(
                    "임시 비밀번호 발급",
                    "안녕하세요 <strong>" + name + "</strong>님,<br><br>요청하신 임시 비밀번호가 발급되었습니다.",
                    "임시 비밀번호",
                    temporaryPassword,
                    "비밀번호를 복사하여 로그인 시 사용하세요.",
                    new String[]{
                            "보안을 위해 로그인 후 반드시 비밀번호를 변경해주세요.",
                            "임시 비밀번호는 한 번만 사용 가능합니다."
                    }
            ), true);

            mailSender.send(message);

            log.info("Temporary password sent to: {}", email);
            return temporaryPassword;
        } catch (MessagingException e) {
            log.error("Failed to send temporary password email to: {}. Error: {}", email, e.getMessage(), e);
            throw new RuntimeException("이메일 발송에 실패했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to send temporary password email to: {}. Error: {}", email, e.getMessage(), e);
            throw new RuntimeException("이메일 발송에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 공통 이메일 템플릿 생성 메서드
     */
    private String createEmailTemplate(String title, String description, String codeLabel, String code,
                                       String copyGuide, String[] warnings) {

        StringBuilder warningsList = new StringBuilder();
        for (String warning : warnings) {
            warningsList.append(String.format("<li>%s</li>", warning));
        }

        String logoUrl = "https://replant-bucket.s3.ap-northeast-2.amazonaws.com/replant/logo/replant_logo.png";
        String doriUrl = "https://replant-bucket.s3.ap-northeast-2.amazonaws.com/replant/logo/replant_viva.png";

        return String.format("""
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin: 0; padding: 0; font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif; background-color: #f5f7fa;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f5f7fa; padding: 40px 0;">
                        <tr>
                            <td align="center">

                                <!-- 메인 컨테이너 -->
                                <table width="600" cellpadding="0" cellspacing="0"
                                       style="background-color: #ffffff; border-radius: 16px;
                                              box-shadow: none;">

                                    <!-- 헤더 (로고) -->
                                    <tr>
                                        <td style="padding: 0; text-align: center; border-radius: 16px 16px 0 0;">

                                            <img src="%s"
                                                 alt="Replant 로고"
                                                 style="width: 100%%; max-width: 600px; height: auto; display: block; margin: 0;" />

                                        </td>
                                    </tr>

                                    <!-- 본문 -->
                                    <tr>
                                        <td style="padding: 70px 30px;">

                                            <!-- 제목 -->
                                            <h2 style="margin: 10px 0 20px 0; color: #1a202c;
                                                       font-size: 22px; font-weight: 600;
                                                       text-align: center;">
                                                %s
                                            </h2>

                                            <!-- 두리 이미지 -->
                                            <img src="%s"
                                                 alt="두리 캐릭터"
                                                 style="width:200px; display:block; margin:0 auto 25px auto;" />

                                            <!-- 설명 -->
                                            <p style="margin: 10px 0 30px 0; color: #4a5568;
                                                      font-size: 15px; line-height: 1.6;
                                                      text-align: center;">
                                                %s
                                            </p>

                                            <!-- 코드 박스 -->
                                            <table width="100%%" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td align="center" style="padding: 30px 0;">

                                                        <div style="background: #CDE8C5;
                                                                    border-radius: 16px;
                                                                    padding: 45px 60px;
                                                                    display: inline-block;
                                                                    box-shadow: none;">

                                                            <p style="margin: 0 0 12px 0; color: #1A1A1A;
                                                                      font-size: 14px; font-weight: 500;">
                                                                %s
                                                            </p>

                                                            <p style="margin: 0; color: #3A4D39;
                                                                      font-size: 38px; font-weight: 700;
                                                                      letter-spacing: 4px;
                                                                      text-shadow: 0 2px 4px rgba(0,0,0,0.1);
                                                                      user-select: all;">
                                                                %s
                                                            </p>

                                                        </div>

                                                        <div style="margin-top: 15px; color: #718096;
                                                                    font-size: 13px;">
                                                            %s
                                                        </div>

                                                    </td>
                                                </tr>
                                            </table>

                                            <!-- 유의사항 -->
                                            <table width="100%%" cellpadding="0" cellspacing="0" style="margin-top: 30px;">
                                                <tr>
                                                    <td width="6" style="background-color: #3A4D39;
                                                                         border-radius: 12px 0 0 12px;"></td>

                                                    <td style="background-color: #f7fafc; padding: 25px;
                                                               border-radius: 0 12px 12px 0;">
                                                        <p style="margin: 0 0 15px 0; color: #2d3748;
                                                                  font-size: 14px; font-weight: 600;">
                                                            ⚠️ 유의사항
                                                        </p>
                                                        <ul style="margin: 0; padding-left: 20px; color: #718096;
                                                                   font-size: 13px; line-height: 2;">
                                                            %s
                                                        </ul>
                                                    </td>
                                                </tr>
                                            </table>

                                        </td>
                                    </tr>

                                    <!-- 푸터 -->
                                    <tr>
                                        <td style="background-color: #f7fafc; padding: 30px;
                                                   text-align: center; border-top: 1px solid #e2e8f0;">
                                            <p style="margin: 0 0 10px 0; color: #a0aec0; font-size: 12px;">
                                                © 2026 Replant
                                            </p>
                                            <p style="margin: 0; color: #cbd5e0; font-size: 11px;">
                                                이 메일은 발신 전용입니다. 문의사항은 고객센터를 이용해주세요. 📧
                                            </p>
                                        </td>
                                    </tr>

                                </table>

                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """,
                logoUrl,          // 헤더 로고
                title,            // 제목
                doriUrl,          // 두리 이미지
                description,      // 설명
                codeLabel,        // 코드 라벨
                code,             // 코드
                copyGuide,        // 복사 안내 문구
                warningsList.toString()  // 유의사항 리스트
        );
    }
}
