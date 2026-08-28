package com.youthexpedition.azit.modules.notification.adapter.out.external;

import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.youthexpedition.azit.modules.notification.application.port.out.PushMessage;
import com.youthexpedition.azit.modules.notification.application.port.out.PushSendResult;
import com.youthexpedition.azit.modules.notification.application.port.out.PushSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class FcmPushAdapter implements PushSenderPort {

    // 토큰이 더 이상 유효하지 않다는 뜻이라 삭제 대상임
    private static final Set<MessagingErrorCode> INVALID_TOKEN_ERRORS =
            EnumSet.of(MessagingErrorCode.UNREGISTERED, MessagingErrorCode.INVALID_ARGUMENT);

    private final FirebaseMessaging firebaseMessaging;

    // 서비스 계정 키가 없으면 FirebaseMessaging 빈이 없으므로 발송을 건너뜀
    public FcmPushAdapter(@Autowired(required = false) FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public PushSendResult send(PushMessage message) {
        if (firebaseMessaging == null) {
            log.warn("[FCM] 발송이 비활성화되어 있어 푸시를 건너뜁니다. title: {}", message.title());
            return PushSendResult.empty();
        }

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(toMulticastMessage(message));
            return toResult(response, message.tokens());
        } catch (FirebaseMessagingException e) {
            log.error("[FCM] 멀티캐스트 발송에 실패했습니다. 대상 토큰 수: {}", message.tokens().size(), e);
            return PushSendResult.of(0, message.tokens().size(), List.of());
        }
    }

    private MulticastMessage toMulticastMessage(PushMessage message) {
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

    private PushSendResult toResult(BatchResponse response, List<String> tokens) {
        List<String> invalidTokens = new ArrayList<>();
        List<SendResponse> responses = response.getResponses();

        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (sendResponse.isSuccessful()) continue;

            FirebaseMessagingException exception = sendResponse.getException();
            if (exception != null && INVALID_TOKEN_ERRORS.contains(exception.getMessagingErrorCode())) {
                invalidTokens.add(tokens.get(i));
            } else {
                log.warn("[FCM] 개별 발송 실패. errorCode: {}",
                        exception == null ? "UNKNOWN" : exception.getMessagingErrorCode());
            }
        }

        return PushSendResult.of(response.getSuccessCount(), response.getFailureCount(), invalidTokens);
    }
}
