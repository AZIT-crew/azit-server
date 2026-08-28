package com.youthexpedition.azit.modules.notification.domain.model;

import com.youthexpedition.azit.modules.notification.domain.model.enums.NotificationOutboxStatus;
import com.youthexpedition.azit.modules.notification.domain.model.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class NotificationOutbox {
    private final Long id;
    private final NotificationType type;
    private final String payload; // 수신자 확정에 필요한 정보 (JSON)
    private NotificationOutboxStatus status;
    private int retryCount;
    private LocalDateTime processedAt;
    private String errorMessage;
    private final LocalDateTime createdAt;

    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;

    public static NotificationOutbox create(NotificationType type, String payload) {
        return NotificationOutbox.builder()
                .type(type)
                .payload(payload)
                .status(NotificationOutboxStatus.PENDING)
                .retryCount(0)
                .build();
    }

    public void markDone(LocalDateTime now) {
        this.status = NotificationOutboxStatus.DONE;
        this.processedAt = now;
        this.errorMessage = null;
    }

    // 처리 실패. 재시도 한도를 넘으면 FAILED 로 두어 폴러가 다시 집지 않게 함
    public void markFailed(LocalDateTime now, String errorMessage, int maxRetryCount) {
        this.retryCount++;
        this.errorMessage = truncate(errorMessage);
        this.processedAt = now;
        this.status = this.retryCount >= maxRetryCount
                ? NotificationOutboxStatus.FAILED
                : NotificationOutboxStatus.PENDING;
    }

    public boolean isPending() {
        return this.status == NotificationOutboxStatus.PENDING;
    }

    private String truncate(String errorMessage) {
        if (errorMessage == null) return null;
        return errorMessage.length() <= ERROR_MESSAGE_MAX_LENGTH
                ? errorMessage
                : errorMessage.substring(0, ERROR_MESSAGE_MAX_LENGTH);
    }
}
