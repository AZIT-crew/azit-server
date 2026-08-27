package com.youthexpedition.azit.modules.notification.adapter.out.persistence;

import com.youthexpedition.azit.modules.notification.adapter.out.mapper.NotificationOutboxMapper;
import com.youthexpedition.azit.modules.notification.adapter.out.persistence.repository.NotificationOutboxRepository;
import com.youthexpedition.azit.modules.notification.application.port.out.LoadNotificationOutboxPort;
import com.youthexpedition.azit.modules.notification.application.port.out.SaveNotificationOutboxPort;
import com.youthexpedition.azit.modules.notification.domain.model.NotificationOutbox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationOutboxPersistenceAdapter implements LoadNotificationOutboxPort, SaveNotificationOutboxPort {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final NotificationOutboxMapper notificationOutboxMapper;

    @Override
    public Optional<NotificationOutbox> claimNext(LocalDateTime retryableBefore) {
        return notificationOutboxRepository.claimNext(retryableBefore)
                .map(notificationOutboxMapper::toDomain);
    }

    @Override
    public void save(NotificationOutbox outbox) {
        notificationOutboxRepository.save(notificationOutboxMapper.toEntity(outbox));
    }
}
