package com.youthexpedition.azit.modules.notification.application.port.out;

import com.youthexpedition.azit.modules.notification.domain.model.NotificationOutbox;

import java.time.LocalDateTime;
import java.util.Optional;

public interface LoadNotificationOutboxPort {
    /**
     * 대기 중인 아웃박스를 한 건 선점함 (다른 인스턴스가 집은 행은 건너뜀).
     * 처리에 실패한 건은 retryableBefore 이전에 시도된 것만 다시 집어, 같은 건을 쉼 없이 재시도하지 않음.
     */
    Optional<NotificationOutbox> claimNext(LocalDateTime retryableBefore);
}
