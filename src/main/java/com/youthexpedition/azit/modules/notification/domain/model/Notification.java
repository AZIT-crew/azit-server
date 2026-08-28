package com.youthexpedition.azit.modules.notification.domain.model;

import com.youthexpedition.azit.modules.notification.domain.model.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class Notification {
    private final Long id;
    private final Long receiverId;
    private final NotificationType type;
    private final String title; // title, body는 알림 생성 시점 기준으로 저장
    private final String body;
    private final Long crewId; // 알림을 탭했을 때 이동할 대상
    private boolean isRead;
    private LocalDateTime readAt;
    private final LocalDateTime createdAt;

    public static Notification create(Long receiverId, NotificationType type, Long crewId, String crewName) {
        return Notification.builder()
                .receiverId(receiverId)
                .type(type)
                .title(type.getTitle())
                .body(type.formatBody(crewName))
                .crewId(crewId)
                .isRead(false)
                .build();
    }

    public void markAsRead(LocalDateTime now) {
        if (this.isRead) return; // 이미 읽은 알림은 읽은 시점을 유지

        this.isRead = true;
        this.readAt = now;
    }
}
