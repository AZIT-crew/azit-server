package com.youthexpedition.azit.modules.notification.adapter.out.mapper;

import com.youthexpedition.azit.modules.notification.adapter.out.persistence.entity.NotificationOutboxEntity;
import com.youthexpedition.azit.modules.notification.domain.model.NotificationOutbox;
import org.springframework.stereotype.Component;

@Component
public class NotificationOutboxMapper {

    public NotificationOutbox toDomain(NotificationOutboxEntity entity) {
        return NotificationOutbox.builder()
                .id(entity.getId())
                .type(entity.getType())
                .payload(entity.getPayload())
                .status(entity.getStatus())
                .retryCount(entity.getRetryCount())
                .processedAt(entity.getProcessedAt())
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public NotificationOutboxEntity toEntity(NotificationOutbox domain) {
        return NotificationOutboxEntity.builder()
                .id(domain.getId())
                .type(domain.getType())
                .payload(domain.getPayload())
                .status(domain.getStatus())
                .retryCount(domain.getRetryCount())
                .processedAt(domain.getProcessedAt())
                .errorMessage(domain.getErrorMessage())
                .build();
    }
}
