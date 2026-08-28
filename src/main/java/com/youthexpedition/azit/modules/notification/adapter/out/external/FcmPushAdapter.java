package com.youthexpedition.azit.modules.notification.adapter.out.external;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.youthexpedition.azit.modules.notification.adapter.out.mapper.FcmMessageMapper;
import com.youthexpedition.azit.modules.notification.application.port.out.PushMessage;
import com.youthexpedition.azit.modules.notification.application.port.out.PushSendResult;
import com.youthexpedition.azit.modules.notification.application.port.out.PushSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class FcmPushAdapter implements PushSenderPort {

    private final FirebaseMessaging firebaseMessaging;
    private final FcmMessageMapper fcmMessageMapper;

    // 서비스 계정 키가 없으면 FirebaseMessaging 빈이 없으므로 발송을 건너뜀
    public FcmPushAdapter(@Autowired(required = false) FirebaseMessaging firebaseMessaging,
                          FcmMessageMapper fcmMessageMapper) {
        this.firebaseMessaging = firebaseMessaging;
        this.fcmMessageMapper = fcmMessageMapper;
    }

    @Override
    public PushSendResult send(PushMessage message) {
        if (firebaseMessaging == null) {
            log.warn("[FCM] 발송이 비활성화되어 있어 푸시를 건너뜁니다. title: {}", message.title());
            return PushSendResult.empty();
        }

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(fcmMessageMapper.toMulticastMessage(message));
            return fcmMessageMapper.toResult(response, message.tokens());
        } catch (FirebaseMessagingException e) {
            log.error("[FCM] 멀티캐스트 발송에 실패했습니다. 대상 토큰 수: {}", message.tokens().size(), e);
            return PushSendResult.of(0, message.tokens().size(), List.of());
        }
    }
}
