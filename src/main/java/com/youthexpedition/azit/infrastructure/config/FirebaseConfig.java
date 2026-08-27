package com.youthexpedition.azit.infrastructure.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * FCM 초기화 config
 * 서비스 계정 키가 없으면 FirebaseMessaging 빈을 만들지 않음
 * 키 없이 뜨는 로컬 환경에서도 애플리케이션이 기동되어야 하고, 이 경우 푸시 발송만 건너뜀
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    // FCM 응답이 늦어질 때 폴러가 오래 물리지 않도록 제한함
    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 10000;

    private final String credentialPath;

    public FirebaseConfig(@Value("${fcm.credential-path:}") String credentialPath) {
        this.credentialPath = credentialPath;
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (!StringUtils.hasText(credentialPath)) {
            log.warn("[FCM] 서비스 계정 키 경로가 설정되지 않아 푸시 발송이 비활성화됩니다.");
            return null;
        }

        try {
            FirebaseApp firebaseApp = initializeFirebaseApp();
            log.info("[FCM] Firebase 초기화 완료");
            return FirebaseMessaging.getInstance(firebaseApp);
        } catch (IOException e) {
            log.error("[FCM] 서비스 계정 키를 읽지 못해 푸시 발송이 비활성화됩니다. path: {}", credentialPath, e);
            return null;
        }
    }

    private FirebaseApp initializeFirebaseApp() throws IOException {
        // 테스트 컨텍스트가 여러 번 뜨는 경우 중복 초기화를 피함
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        Resource resource = new FileSystemResource(credentialPath);
        try (InputStream credentialStream = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialStream))
                    .setConnectTimeout(CONNECT_TIMEOUT_MS)
                    .setReadTimeout(READ_TIMEOUT_MS)
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }
}
