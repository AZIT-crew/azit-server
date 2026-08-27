package com.youthexpedition.azit.modules.notification.application.port.out;

import com.youthexpedition.azit.modules.notification.domain.model.NotificationOutbox;

public interface SaveNotificationOutboxPort {
    void save(NotificationOutbox outbox);
}
