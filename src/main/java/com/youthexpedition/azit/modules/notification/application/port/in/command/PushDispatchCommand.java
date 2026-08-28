package com.youthexpedition.azit.modules.notification.application.port.in.command;

import com.youthexpedition.azit.modules.notification.domain.model.enums.NotificationType;

import java.util.List;

/**
 * 푸시 발송 대상. 전체 알림(알림 수신 동의)이 켜진 수신자만 담김.
 * 인앱 알림은 동의 여부와 무관하게 저장되므로 이 목록과 다를 수 있음.
 *
 * @param badgeCount iOS 배지에 표시할 수신자의 안 읽은 알림 개수
 */
public record PushDispatchCommand(
        List<Long> receiverIds,
        NotificationType type,
        String title,
        String body,
        Long crewId,
        int badgeCount
) {
    public static PushDispatchCommand of(List<Long> receiverIds, NotificationType type,
                                         String title, String body, Long crewId, int badgeCount) {
        return new PushDispatchCommand(receiverIds, type, title, body, crewId, badgeCount);
    }

    public boolean hasReceiver() {
        return !receiverIds.isEmpty();
    }
}
