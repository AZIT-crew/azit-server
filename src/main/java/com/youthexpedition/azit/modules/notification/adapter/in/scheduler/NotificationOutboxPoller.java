package com.youthexpedition.azit.modules.notification.adapter.in.scheduler;

import com.youthexpedition.azit.modules.notification.application.port.in.DispatchPushUseCase;
import com.youthexpedition.azit.modules.notification.application.port.in.ProcessNotificationOutboxUseCase;
import com.youthexpedition.azit.modules.notification.application.port.in.command.PushDispatchCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 아웃박스를 주기적으로 확인해 알림을 생성하고 푸시를 발송함.
 * 알림 생성(트랜잭션)이 끝난 뒤에 푸시를 보내므로, 푸시가 실패해도 알림함에는 남음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxPoller {

    private final ProcessNotificationOutboxUseCase processNotificationOutboxUseCase;
    private final DispatchPushUseCase dispatchPushUseCase;

    @Scheduled(fixedDelayString = "${fcm.outbox.poll-delay-ms:3000}")
    public void poll() {
        try {
            List<PushDispatchCommand> dispatches = processNotificationOutboxUseCase.processPending();
            dispatches.forEach(dispatchPushUseCase::dispatch);
        } catch (Exception e) {
            // 폴링 주기마다 다시 시도하므로 예외를 삼켜 스케줄러가 멈추지 않게 함
            log.error("[NOTIFICATION] 아웃박스 폴링 중 오류가 발생했습니다.", e);
        }
    }
}
