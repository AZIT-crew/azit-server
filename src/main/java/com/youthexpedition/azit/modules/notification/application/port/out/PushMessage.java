package com.youthexpedition.azit.modules.notification.application.port.out;

import com.youthexpedition.azit.modules.notification.domain.model.enums.NotificationType;

import java.util.List;

public record PushMessage(
        List<String> tokens,
        String title,
        String body,
        NotificationType type,
        Long crewId
) {
    public static PushMessage of(List<String> tokens, String title, String body,
                                 NotificationType type, Long crewId) {
        return new PushMessage(tokens, title, body, type, crewId);
    }
}
