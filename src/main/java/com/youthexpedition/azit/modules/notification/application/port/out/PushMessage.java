package com.youthexpedition.azit.modules.notification.application.port.out;

import com.youthexpedition.azit.modules.notification.domain.model.enums.NotificationType;

import java.util.List;

/**
 * @param badgeCount iOS 배지에 표시할 안 읽은 알림 개수
 */
public record PushMessage(
        List<String> tokens,
        String title,
        String body,
        NotificationType type,
        Long crewId,
        int badgeCount
) {
    public static PushMessage of(List<String> tokens, String title, String body,
                                 NotificationType type, Long crewId, int badgeCount) {
        return new PushMessage(tokens, title, body, type, crewId, badgeCount);
    }
}
