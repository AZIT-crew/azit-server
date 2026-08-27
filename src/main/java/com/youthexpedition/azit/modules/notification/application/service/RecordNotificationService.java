package com.youthexpedition.azit.modules.notification.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youthexpedition.azit.infrastructure.exception.BusinessException;
import com.youthexpedition.azit.modules.notification.application.port.in.RecordNotificationUseCase;
import com.youthexpedition.azit.modules.notification.application.port.out.SaveNotificationOutboxPort;
import com.youthexpedition.azit.modules.notification.application.service.dto.CrewJoinNotificationPayload;
import com.youthexpedition.azit.modules.notification.domain.model.NotificationOutbox;
import com.youthexpedition.azit.modules.notification.domain.model.enums.NotificationErrorCode;
import com.youthexpedition.azit.modules.notification.domain.model.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 호출자(가입 요청·승인·거절)의 트랜잭션에 참여해 아웃박스를 남김.
 * 비즈니스 처리가 롤백되면 알림 요청도 함께 사라져야 하므로 새 트랜잭션을 열지 않음.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class RecordNotificationService implements RecordNotificationUseCase {

    private final SaveNotificationOutboxPort saveNotificationOutboxPort;
    private final ObjectMapper objectMapper;

    @Override
    public void recordCrewJoinRequested(Long crewId, Long requesterMemberId) {
        record(NotificationType.CREW_JOIN_REQUESTED, crewId, requesterMemberId);
    }

    @Override
    public void recordCrewJoinApproved(Long crewId, Long targetMemberId) {
        record(NotificationType.CREW_JOIN_APPROVED, crewId, targetMemberId);
    }

    @Override
    public void recordCrewJoinRejected(Long crewId, Long targetMemberId) {
        record(NotificationType.CREW_JOIN_REJECTED, crewId, targetMemberId);
    }

    private void record(NotificationType type, Long crewId, Long memberId) {
        String payload = serialize(CrewJoinNotificationPayload.of(crewId, memberId));
        saveNotificationOutboxPort.save(NotificationOutbox.create(type, payload));

        log.info("[NOTIFICATION] {} 알림 발송을 요청합니다. crewId: {}, memberId: {}", type, crewId, memberId);
    }

    private String serialize(CrewJoinNotificationPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("[NOTIFICATION] 알림 payload 직렬화에 실패했습니다. payload: {}", payload, e);
            throw new BusinessException(NotificationErrorCode.INVALID_NOTIFICATION_PAYLOAD);
        }
    }
}
