package com.youthexpedition.azit.modules.notification.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youthexpedition.azit.infrastructure.exception.BusinessException;
import com.youthexpedition.azit.modules.crew.application.port.out.LoadCrewMemberPort;
import com.youthexpedition.azit.modules.crew.application.port.out.LoadCrewPort;
import com.youthexpedition.azit.modules.crew.domain.model.Crew;
import com.youthexpedition.azit.modules.member.application.port.out.LoadMemberPort;
import com.youthexpedition.azit.modules.member.domain.model.Member;
import com.youthexpedition.azit.modules.notification.application.port.in.command.PushDispatchCommand;
import com.youthexpedition.azit.modules.notification.application.port.out.LoadNotificationOutboxPort;
import com.youthexpedition.azit.modules.notification.application.port.out.SaveNotificationOutboxPort;
import com.youthexpedition.azit.modules.notification.application.port.out.SaveNotificationPort;
import com.youthexpedition.azit.modules.notification.application.service.dto.CrewJoinNotificationPayload;
import com.youthexpedition.azit.modules.notification.application.service.dto.OutboxProcessResult;
import com.youthexpedition.azit.modules.notification.domain.model.Notification;
import com.youthexpedition.azit.modules.notification.domain.model.NotificationOutbox;
import com.youthexpedition.azit.modules.notification.domain.model.enums.NotificationErrorCode;
import com.youthexpedition.azit.modules.notification.domain.model.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 아웃박스를 한 건씩 선점해 인앱 알림을 저장하고 푸시 발송 대상을 만듦.
 * 건별로 트랜잭션을 끊으므로 한 건이 실패해도 나머지 처리 결과가 함께 롤백되지 않고, 실패 횟수도 정상적으로 누적됨.
 * 푸시 발송은 이 트랜잭션이 끝난 뒤 호출자가 수행함.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutboxProcessor {

    private final LoadNotificationOutboxPort loadNotificationOutboxPort;
    private final SaveNotificationOutboxPort saveNotificationOutboxPort;
    private final SaveNotificationPort saveNotificationPort;
    private final LoadCrewPort loadCrewPort;
    private final LoadCrewMemberPort loadCrewMemberPort;
    private final LoadMemberPort loadMemberPort;
    private final ObjectMapper objectMapper;

    @Value("${fcm.outbox.max-retry-count:5}")
    private int maxRetryCount;

    @Value("${fcm.outbox.retry-backoff-seconds:60}")
    private int retryBackoffSeconds;

    /**
     * 대기 중인 아웃박스 한 건을 처리함.
     * 처리할 건이 없거나 푸시 대상이 없으면 비어 있는 결과를 반환함.
     */
    @Transactional
    public OutboxProcessResult processNext() {
        LocalDateTime now = LocalDateTime.now();
        Optional<NotificationOutbox> claimed = loadNotificationOutboxPort.claimNext(now.minusSeconds(retryBackoffSeconds));
        if (claimed.isEmpty()) return OutboxProcessResult.nothingToProcess();

        NotificationOutbox outbox = claimed.get();
        try {
            Notification notification = toNotification(outbox);
            saveNotificationPort.saveAll(List.of(notification));
            outbox.markDone(now);
            saveNotificationOutboxPort.save(outbox);

            return OutboxProcessResult.processed(toDispatchCommand(notification));
        } catch (Exception e) {
            // 이 건만 실패로 기록함. 재시도 한도를 넘으면 FAILED 로 바뀌어 다시 선점되지 않음
            outbox.markFailed(now, e.getMessage(), maxRetryCount);
            saveNotificationOutboxPort.save(outbox);
            log.error("[NOTIFICATION] 아웃박스 처리에 실패했습니다. outboxId: {}, retryCount: {}, status: {}",
                    outbox.getId(), outbox.getRetryCount(), outbox.getStatus(), e);

            return OutboxProcessResult.processed(null); // 실패도 이번 사이클에서 처리한 건으로 셈
        }
    }

    private Notification toNotification(NotificationOutbox outbox) {
        CrewJoinNotificationPayload payload = deserialize(outbox.getPayload());
        Crew crew = loadCrewPort.findById(payload.crewId())
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_RECEIVER_NOT_FOUND));

        Long receiverId = resolveReceiverId(outbox.getType(), payload);

        return Notification.create(receiverId, outbox.getType(), crew.getId(), crew.getName());
    }

    // 가입 요청은 그 시점의 크루 리더에게, 승인·거절은 신청자에게 감
    private Long resolveReceiverId(NotificationType type, CrewJoinNotificationPayload payload) {
        if (type != NotificationType.CREW_JOIN_REQUESTED) {
            return payload.memberId();
        }

        return loadCrewMemberPort.findLeaderByCrewId(payload.crewId())
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_RECEIVER_NOT_FOUND))
                .getMemberId();
    }

    // 인앱 알림은 전원 저장하되, 푸시는 전체 알림(알림 수신 동의)이 켜진 회원에게만 보냄
    private PushDispatchCommand toDispatchCommand(Notification notification) {
        return loadMemberPort.findById(notification.getReceiverId())
                .filter(Member::isNotificationAgreed)
                .map(member -> PushDispatchCommand.of(List.of(member.getId()), notification.getType(),
                        notification.getTitle(), notification.getBody(), notification.getCrewId()))
                .orElse(null);
    }

    private CrewJoinNotificationPayload deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, CrewJoinNotificationPayload.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(NotificationErrorCode.INVALID_NOTIFICATION_PAYLOAD);
        }
    }
}
