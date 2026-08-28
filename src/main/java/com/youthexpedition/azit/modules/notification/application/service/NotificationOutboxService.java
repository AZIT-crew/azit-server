package com.youthexpedition.azit.modules.notification.application.service;

import com.youthexpedition.azit.modules.notification.application.port.in.ProcessNotificationOutboxUseCase;
import com.youthexpedition.azit.modules.notification.application.port.in.command.PushDispatchCommand;
import com.youthexpedition.azit.modules.notification.application.service.dto.OutboxProcessResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 한 폴링 사이클에서 처리할 아웃박스 건수를 제한하고, 처리량과 소요 시간을 남김.
 * 실제 처리는 건별 트랜잭션으로 NotificationOutboxProcessor 가 수행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutboxService implements ProcessNotificationOutboxUseCase {

    private final NotificationOutboxProcessor notificationOutboxProcessor;

    @Value("${fcm.outbox.claim-size:100}")
    private int claimSize;

    @Override
    public List<PushDispatchCommand> processPending() {
        long startedAt = System.currentTimeMillis();
        List<PushDispatchCommand> dispatches = new ArrayList<>();
        int processedCount = 0;

        // 사이클당 처리량 제한, 남은 건은 다음 사이클에서 이어서 처리됨
        // 추후 부하 발생 시 claimSize 줄일지 확인 필요
        for (int count = 0; count < claimSize; count++) {
            OutboxProcessResult result = notificationOutboxProcessor.processNext();
            if (!result.processed()) break; // 처리할 건이 없으면 사이클 종료

            processedCount++;
            if (result.hasPushTarget()) {
                dispatches.add(result.pushTarget());
            }
        }

        if (processedCount > 0) {
            log.info("[NOTIFICATION] 아웃박스 {}건 처리 완료. 푸시 대상 {}건, 소요 {}ms",
                    processedCount, dispatches.size(), System.currentTimeMillis() - startedAt);
        }

        return dispatches;
    }
}
