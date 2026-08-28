package com.youthexpedition.azit.modules.notification.adapter.out.mapper;

import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.youthexpedition.azit.modules.notification.application.port.out.PushMessage;
import com.youthexpedition.azit.modules.notification.application.port.out.PushSendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 애플리케이션의 푸시 메시지와 FCM 타입 사이의 변환을 담당함.
 * Firebase 타입을 이 클래스 안에 가둬, 벤더가 바뀌어도 애플리케이션 계층은 영향을 받지 않음.
 */
@Slf4j
@Component
public class FcmMessageMapper {

    // 토큰이 더 이상 유효하지 않다는 뜻이라 삭제 대상임
    private static final Set<MessagingErrorCode> INVALID_TOKEN_ERRORS =
            EnumSet.of(MessagingErrorCode.UNREGISTERED, MessagingErrorCode.INVALID_ARGUMENT);

    public MulticastMessage toMulticastMessage(PushMessage message) {
        return MulticastMessage.builder()
                .addAllTokens(message.tokens())
                .setNotification(Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .setApnsConfig(toApnsConfig(message))
                .putAllData(toData(message))
                .build();
    }

    public PushSendResult toResult(BatchResponse response, List<String> tokens) {
        List<String> invalidTokens = new ArrayList<>();
        List<SendResponse> responses = response.getResponses();

        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (sendResponse.isSuccessful()) continue;

            FirebaseMessagingException exception = sendResponse.getException();
            if (exception != null && INVALID_TOKEN_ERRORS.contains(exception.getMessagingErrorCode())) {
                invalidTokens.add(tokens.get(i)); // 응답 순서가 토큰 순서와 같아 인덱스로 짝지음
            } else {
                log.warn("[FCM] 개별 발송 실패. errorCode: {}",
                        exception == null ? "UNKNOWN" : exception.getMessagingErrorCode());
            }
        }

        return PushSendResult.of(response.getSuccessCount(), response.getFailureCount(), invalidTokens);
    }

    // iOS 배지. 안드로이드는 이 설정을 무시하므로 플랫폼별로 나눠 보낼 필요가 없음
    private ApnsConfig toApnsConfig(PushMessage message) {
        return ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setBadge(message.badgeCount())
                        .build())
                .build();
    }

    // 클라이언트가 알림을 탭했을 때 이동할 화면을 판단하는 데 사용함
    private Map<String, String> toData(PushMessage message) {
        Map<String, String> data = new HashMap<>();
        data.put("type", message.type().name());
        if (message.crewId() != null) {
            data.put("crewId", String.valueOf(message.crewId()));
        }
        return data;
    }
}
