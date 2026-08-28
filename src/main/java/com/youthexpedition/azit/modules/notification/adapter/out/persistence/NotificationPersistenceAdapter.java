package com.youthexpedition.azit.modules.notification.adapter.out.persistence;

import com.youthexpedition.azit.modules.notification.adapter.out.mapper.NotificationMapper;
import com.youthexpedition.azit.modules.notification.adapter.out.persistence.entity.NotificationEntity;
import com.youthexpedition.azit.modules.notification.adapter.out.persistence.repository.NotificationRepository;
import com.youthexpedition.azit.modules.notification.application.port.out.LoadNotificationPort;
import com.youthexpedition.azit.modules.notification.application.port.out.SaveNotificationPort;
import com.youthexpedition.azit.modules.notification.domain.model.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationPersistenceAdapter implements SaveNotificationPort, LoadNotificationPort {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public long countUnreadByReceiverId(Long receiverId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(receiverId);
    }

    @Override
    public void saveAll(List<Notification> notifications) {
        if (notifications.isEmpty()) return;

        List<NotificationEntity> entities = notifications.stream()
                .map(notificationMapper::toEntity)
                .toList();
        notificationRepository.saveAll(entities);
    }
}
