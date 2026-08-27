package com.youthexpedition.azit.modules.notification.application.port.in.command;

import com.youthexpedition.azit.modules.notification.domain.model.enums.NotificationType;

import java.util.List;

/**
 * 푸시 발송 대상. 전체 알림(알림 수신 동의)이 켜진 수신자만 담김.
 * 인앱 알림은 동의 여부와 무관하게 저장되므로 이 목록과 다를 수 있음.
 */
public record PushDispatchCommand(
        List<Long> receiverIds,
        NotificationType type,
        String title,
        String body,
        Long crewId
) {
    public static PushDispatchCommand of(List<Long> receiverIds, NotificationType type,
                                         String title, String body, Long crewId) {
        return new PushDispatchCommand(receiverIds, type, title, body, crewId);
    }

    public boolean hasReceiver() {
        return !receiverIds.isEmpty();
    }
}
