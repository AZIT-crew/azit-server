package com.youthexpedition.azit.modules.notification.application.port.out;

import com.youthexpedition.azit.modules.notification.domain.model.Notification;

import java.util.List;

public interface SaveNotificationPort {
    void saveAll(List<Notification> notifications);
}
