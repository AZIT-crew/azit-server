package com.youthexpedition.azit.modules.notification.domain.model.enums;

public enum NotificationOutboxStatus {
    PENDING, // 발송 대기
    DONE,    // 알림 생성 완료
    FAILED   // 재시도 한도 초과
}
