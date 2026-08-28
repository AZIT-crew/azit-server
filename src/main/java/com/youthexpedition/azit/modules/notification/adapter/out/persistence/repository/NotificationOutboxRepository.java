package com.youthexpedition.azit.modules.notification.adapter.out.persistence.repository;

import com.youthexpedition.azit.modules.notification.adapter.out.persistence.entity.NotificationOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutboxEntity, Long> {

    /**
     * 대기 중인 아웃박스를 한 건 선점함.
     * SKIP LOCKED 로 다른 인스턴스가 이미 잡은 행을 건너뛰므로 중복 처리가 발생하지 않음.
     * 직전에 실패한 건은 재시도 대기 시간이 지난 뒤에만 다시 대상이 됨.
     */
    @Query(value = """
            SELECT * FROM notification_outbox
            WHERE status = 'PENDING'
              AND (processed_at IS NULL OR processed_at <= :retryableBefore)
            ORDER BY id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<NotificationOutboxEntity> claimNext(@Param("retryableBefore") LocalDateTime retryableBefore);
}
