package com.youthexpedition.azit.modules.notification.adapter.out.persistence.repository;

import com.youthexpedition.azit.modules.notification.adapter.out.persistence.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
}
