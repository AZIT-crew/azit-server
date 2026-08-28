package com.youthexpedition.azit.modules.notification.application.port.in.command;

import com.youthexpedition.azit.modules.notification.domain.model.enums.NotificationType;

import java.util.List;

public record PushDispatchCommand(
        List<Long> receiverIds,
        NotificationType type,
        String title,
        String body,
        Long crewId,
        int badgeCount // iOS 배지에 표시할 수신자의 안 읽은 알림 개수
) {
    public static PushDispatchCommand of(List<Long> receiverIds, NotificationType type,
                                         String title, String body, Long crewId, int badgeCount) {
        return new PushDispatchCommand(receiverIds, type, title, body, crewId, badgeCount);
    }

    public boolean hasReceiver() {
        return !receiverIds.isEmpty();
    }
}
