package com.app.replant.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.config.path:firebase/replant-application-firebase-adminsdk-fbsvc-639f320f0c.json}")
    private String firebaseConfigPath;

    @PostConstruct
    public void initialize() {
        try {
            // Firebase가 이미 초기화되어 있는지 확인
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount;

                try {
                    // 1. classpath에서 찾기 (resources 폴더)
                    serviceAccount = new ClassPathResource(firebaseConfigPath).getInputStream();
                    log.info("[Firebase] Classpath에서 설정 파일 로드 성공: {}", firebaseConfigPath);
                } catch (IOException e) {
                    // 2. 절대 경로에서 찾기
                    log.warn("[Firebase] Classpath에서 설정 파일을 찾을 수 없음. 절대 경로 시도: {}", firebaseConfigPath);
                    serviceAccount = new FileInputStream(firebaseConfigPath);
                    log.info("[Firebase] 절대 경로에서 설정 파일 로드 성공: {}", firebaseConfigPath);
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("[Firebase] Firebase Admin SDK 초기화 완료");
            } else {
                log.info("[Firebase] Firebase는 이미 초기화되어 있습니다.");
            }
        } catch (IOException e) {
            log.error("[Firebase] Firebase 초기화 실패 - 설정 파일을 찾을 수 없습니다: {}", firebaseConfigPath, e);
            log.error("[Firebase] FCM 푸시 알림 기능이 비활성화됩니다.");
        } catch (Exception e) {
            log.error("[Firebase] Firebase 초기화 중 예외 발생", e);
            log.error("[Firebase] FCM 푸시 알림 기능이 비활성화됩니다.");
        }
    }
}
