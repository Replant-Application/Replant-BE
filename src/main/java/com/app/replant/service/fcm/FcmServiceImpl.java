package com.app.replant.service.fcm;

import com.app.replant.domain.notification.dto.FcmMessageDto;
import com.app.replant.domain.notification.dto.FcmSendDto;
import com.app.replant.domain.notification.entity.Notification;
import com.app.replant.domain.user.entity.User;
import com.app.replant.domain.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * FCM SERVICE 구현체
 * FCM과 통신하여 모바일에서 받은 정보를 기반으로 메시지를 전송합니다.
 *
 * @author : lee
 * @fileName : FcmServiceImpl
 * @since : 2/21/24
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FcmServiceImpl implements FcmService {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    
    private static final int MAX_RETRY_ATTEMPTS = 3; // 최대 재시도 횟수
    private static final long RETRY_DELAY_MS = 1000; // 재시도 간격 (1초)
    private static final String FIREBASE_CONFIG_PATH = "firebase/replant-application-firebase-adminsdk-fbsvc-639f320f0c.json";
    private static final String FCM_API_URL = "https://fcm.googleapis.com/v1/projects/replant-application/messages:send";
    private static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";
    
    /**
     * FCM 토큰 마스킹 (로그 보안)
     * @param token FCM 토큰
     * @return 마스킹된 토큰 (앞 10자리 + ... + 뒤 10자리)
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 20) {
            return "***";
        }
        int visibleLength = 10;
        return token.substring(0, visibleLength) + "..." + token.substring(token.length() - visibleLength);
    }
    
    /**
     * FCM 토큰 무효화 처리 (User 테이블에서 삭제)
     * @param userId 사용자 ID
     */
    @Transactional
    private void invalidateToken(Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.updateFcmToken(null);
                log.info("[FCM] 유효하지 않은 토큰 삭제 완료 - userId: {}", userId);
            }
        } catch (Exception e) {
            log.error("[FCM] 토큰 삭제 실패 - userId: {}", userId, e);
        }
    }

    /**
     * 푸시 메시지 처리를 수행하는 비즈니스 로직
     * 메시지를 구성하고 토큰을 받아서 FCM으로 메시지 처리를 수행합니다.
     *
     * @param fcmSendDto 모바일에서 전달받은 Object
     * @return 성공(1), 실패(0)
     * @throws IOException IO 예외
     */
    @Override
    public int sendMessageTo(FcmSendDto fcmSendDto) throws IOException {
        try {
            String message = makeMessage(fcmSendDto);
            RestTemplate restTemplate = new RestTemplate();
            
            /**
             * 추가된 사항 : RestTemplate 이용중 클라이언트의 한글 깨짐 증상에 대한 수정
             * @reference : https://stackoverflow.com/questions/29392422/how-can-i-tell-resttemplate-to-post-with-utf-8-encoding
             */
            restTemplate.getMessageConverters()
                    .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + getAccessToken());

            HttpEntity<String> entity = new HttpEntity<>(message, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    FCM_API_URL, 
                    HttpMethod.POST, 
                    entity, 
                    String.class
            );

            String responseBody = response.getBody();
            log.info("[FCM] 푸시 메시지 전송 응답 상태: {}, 응답 본문: {}", response.getStatusCode(), responseBody);
            
            // FCM API 응답 확인
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null) {
                // 응답 본문에 name 필드가 있으면 성공 (FCM은 성공 시 message name을 반환)
                if (responseBody.contains("\"name\"")) {
                    log.info("[FCM] 푸시 메시지 전송 성공 - 응답: {}", responseBody);
                    return 1;
                } else {
                    log.warn("[FCM] 푸시 메시지 전송 응답이 예상과 다름 - 응답: {}", responseBody);
                    return 0;
                }
            }
            
            return 0;

        } catch (HttpClientErrorException e) {
            // FCM API 에러 응답 처리
            String errorBody = e.getResponseBodyAsString();
            log.error("[FCM] 푸시 메시지 전송 실패 - HTTP Status: {}, Error: {}", e.getStatusCode(), errorBody);
            
            // 유효하지 않은 토큰인 경우 (400 Bad Request, INVALID_ARGUMENT)
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST && 
                errorBody != null && (errorBody.contains("INVALID_ARGUMENT") || errorBody.contains("UNREGISTERED"))) {
                log.warn("[FCM] 유효하지 않은 FCM 토큰 - token: {}", maskToken(fcmSendDto.getToken()));
                // 토큰이 유효하지 않으므로 User 테이블에서 제거할 수 있지만, 
                // 여기서는 userId를 알 수 없으므로 Controller에서 처리하도록 함
            }
            return 0;
        } catch (Exception e) {
            log.error("[FCM] 푸시 메시지 전송 중 예외 발생", e);
            return 0;
        }
    }

    /**
     * Firebase Admin SDK의 비공개 키를 참조하여 Bearer 토큰을 발급 받습니다.
     *
     * @return Bearer token
     * @throws IOException IO 예외
     */
    private String getAccessToken() throws IOException {
        try {
            GoogleCredentials googleCredentials = GoogleCredentials
                    .fromStream(new ClassPathResource(FIREBASE_CONFIG_PATH).getInputStream())
                    .createScoped(List.of(CLOUD_PLATFORM_SCOPE));

            googleCredentials.refreshIfExpired();
            String accessToken = googleCredentials.getAccessToken().getTokenValue();
            log.debug("[FCM] Access Token 발급 성공");
            return accessToken;
        } catch (IOException e) {
            log.error("[FCM] Access Token 발급 실패", e);
            throw new IOException("Firebase Access Token 발급 실패", e);
        }
    }

    /**
     * FCM 전송 정보를 기반으로 메시지를 구성합니다. (Object -> String)
     *
     * @param fcmSendDto FcmSendDto
     * @return String (JSON 문자열)
     * @throws JsonProcessingException JSON 변환 실패 시
     */
    private String makeMessage(FcmSendDto fcmSendDto) throws JsonProcessingException {
        try {
            // data 필드 추가 (Android 앱이 포그라운드에 있을 때도 알림 표시를 위해)
            Map<String, String> data = new HashMap<>();
            data.put("title", fcmSendDto.getTitle());
            data.put("body", fcmSendDto.getBody());
            data.put("click_action", "FLUTTER_NOTIFICATION_CLICK"); // Flutter 앱의 경우
            
            FcmMessageDto fcmMessageDto = FcmMessageDto.builder()
                    .message(FcmMessageDto.Message.builder()
                            .token(fcmSendDto.getToken())
                            .notification(FcmMessageDto.Notification.builder()
                                    .title(fcmSendDto.getTitle())
                                    .body(fcmSendDto.getBody())
                                    .image(null)
                                    .build())
                            .data(data) // data 필드 추가
                            .build())
                    .validateOnly(false)
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(fcmMessageDto);
            // 로그에서 토큰 마스킹
            String maskedJson = jsonMessage.replaceAll(
                "\"token\"\\s*:\\s*\"([^\"]+)\"", 
                "\"token\":\"" + maskToken(fcmSendDto.getToken()) + "\""
            );
            log.info("[FCM] 메시지 구성 완료 - JSON: {}", maskedJson);
            return jsonMessage;
        } catch (JsonProcessingException e) {
            log.error("[FCM] 메시지 구성 실패", e);
            throw e;
        }
    }

    /**
     * 특정 사용자에게 FCM 푸시 알림 전송
     *
     * @param userId       수신자 ID
     * @param notification 알림 엔티티
     * @return 전송 성공 여부
     */
    @Override
    public boolean sendNotification(Long userId, Notification notification) {
        try {
            // 1. User 테이블에서 FCM 토큰 조회
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty() || userOpt.get().getFcmToken() == null || userOpt.get().getFcmToken().isEmpty()) {
                log.warn("[FCM] 사용자 FCM 토큰 없음 - userId: {}", userId);
                return false;
            }

            String token = userOpt.get().getFcmToken();

            // 2. FCM 메시지 생성
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(notification.getTitle())
                            .setBody(notification.getContent())
                            .build())
                    .putAllData(buildNotificationData(notification))
                    .setAndroidConfig(AndroidConfig.builder()
                            .setNotification(AndroidNotification.builder()
                                    .setColor("#023c69") // 앱 primary 색상
                                    .setSound("default")
                                    .build())
                            .build())
                    .build();

            // 3. FCM 전송
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("[FCM] 알림 전송 성공 - userId: {}, messageId: {}", userId, response);
            return true;

        } catch (FirebaseMessagingException e) {
            log.error("[FCM] 알림 전송 실패 - userId: {}, error: {}, errorCode: {}", 
                    userId, e.getMessage(), e.getMessagingErrorCode());

            // 토큰이 유효하지 않은 경우 User 테이블에서 제거
            MessagingErrorCode errorCode = e.getMessagingErrorCode();
            if (errorCode == MessagingErrorCode.INVALID_ARGUMENT ||
                errorCode == MessagingErrorCode.UNREGISTERED) {
                log.warn("[FCM] 유효하지 않은 토큰 감지 - userId: {}, errorCode: {}, token: {}", 
                        userId, errorCode, maskToken(getUserToken(userId)));
                invalidateToken(userId);
            }
            return false;
        } catch (Exception e) {
            log.error("[FCM] 알림 전송 중 예외 발생 - userId: {}", userId, e);
            return false;
        }
    }
    
    /**
     * 사용자 FCM 토큰 조회 (내부용)
     */
    private String getUserToken(Long userId) {
        return userRepository.findById(userId)
                .map(User::getFcmToken)
                .orElse(null);
    }

    /**
     * 알림 데이터 생성 (앱에서 알림 클릭 시 사용)
     */
    private Map<String, String> buildNotificationData(Notification notification) {
        Map<String, String> data = new HashMap<>();
        data.put("id", String.valueOf(notification.getId()));
        data.put("type", notification.getType());
        data.put("title", notification.getTitle());
        data.put("content", notification.getContent());

        if (notification.getReferenceType() != null) {
            data.put("referenceType", notification.getReferenceType());
        }
        if (notification.getReferenceId() != null) {
            data.put("referenceId", String.valueOf(notification.getReferenceId()));
        }

        return data;
    }

    /**
     * 커스텀 FCM 알림 전송
     *
     * @param userId 수신자 ID
     * @param title  제목
     * @param body   내용
     * @param data   추가 데이터
     * @return 전송 성공 여부
     */
    @Override
    public boolean sendCustomNotification(Long userId, String title, String body, Map<String, String> data) {
        try {
            // User 테이블에서 FCM 토큰 조회
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty() || userOpt.get().getFcmToken() == null || userOpt.get().getFcmToken().isEmpty()) {
                log.warn("[FCM] 사용자 FCM 토큰 없음 - userId: {}", userId);
                return false;
            }

            String token = userOpt.get().getFcmToken();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(token)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setNotification(AndroidNotification.builder()
                                    .setColor("#023c69")
                                    .setSound("default")
                                    .build())
                            .build());

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("[FCM] 커스텀 알림 전송 성공 - userId: {}, messageId: {}", userId, response);
            return true;

        } catch (FirebaseMessagingException e) {
            log.error("[FCM] 커스텀 알림 전송 실패 - userId: {}, error: {}, errorCode: {}", 
                    userId, e.getMessage(), e.getMessagingErrorCode());

            // 토큰이 유효하지 않은 경우 User 테이블에서 제거
            MessagingErrorCode errorCode = e.getMessagingErrorCode();
            if (errorCode == MessagingErrorCode.INVALID_ARGUMENT ||
                errorCode == MessagingErrorCode.UNREGISTERED) {
                log.warn("[FCM] 유효하지 않은 토큰 감지 - userId: {}, errorCode: {}, token: {}", 
                        userId, errorCode, maskToken(getUserToken(userId)));
                invalidateToken(userId);
            }
            return false;
        } catch (Exception e) {
            log.error("[FCM] 커스텀 알림 전송 중 예외 발생 - userId: {}", userId, e);
            return false;
        }
    }

    /**
     * FCM 알림 전송 (재시도 로직 포함)
     * FCM 송신 실패 시 재시도 로직 수행
     *
     * @param userId       수신자 ID
     * @param notification 알림 엔티티
     * @return 전송 성공 여부
     */
    @Override
    public boolean sendNotificationWithRetry(Long userId, Notification notification) {
        int attempt = 0;
        boolean success = false;
        
        while (attempt < MAX_RETRY_ATTEMPTS && !success) {
            attempt++;
            
            if (attempt > 1) {
                log.info("[FCM] 재시도 시도 - userId: {}, attempt: {}/{}", userId, attempt, MAX_RETRY_ATTEMPTS);
                try {
                    // 재시도 전 대기
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("[FCM] 재시도 대기 중 인터럽트 발생 - userId: {}", userId);
                    return false;
                }
            }
            
            success = sendNotification(userId, notification);
            
            if (success) {
                log.info("[FCM] 알림 전송 성공 (시도 횟수: {}) - userId: {}", attempt, userId);
                return true;
            } else {
                log.warn("[FCM] 알림 전송 실패 (시도 횟수: {}) - userId: {}", attempt, userId);
            }
        }
        
        log.error("[FCM] 알림 전송 최종 실패 (최대 재시도 횟수 초과) - userId: {}, 총 시도 횟수: {}", userId, attempt);
        return false;
    }
}
