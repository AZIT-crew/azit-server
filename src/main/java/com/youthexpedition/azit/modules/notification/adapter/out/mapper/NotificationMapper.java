package com.youthexpedition.azit.modules.notification.adapter.out.mapper;

import com.youthexpedition.azit.modules.notification.adapter.out.persistence.entity.NotificationEntity;
import com.youthexpedition.azit.modules.notification.domain.model.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public Notification toDomain(NotificationEntity entity) {
        return Notification.builder()
                .id(entity.getId())
                .receiverId(entity.getReceiverId())
                .type(entity.getType())
                .title(entity.getTitle())
                .body(entity.getBody())
                .crewId(entity.getCrewId())
                .isRead(entity.isRead())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public NotificationEntity toEntity(Notification domain) {
        return NotificationEntity.builder()
                .id(domain.getId())
                .receiverId(domain.getReceiverId())
                .type(domain.getType())
                .title(domain.getTitle())
                .body(domain.getBody())
                .crewId(domain.getCrewId())
                .isRead(domain.isRead())
                .readAt(domain.getReadAt())
                .build();
    }
}
