package com.youthexpedition.azit.modules.notification.application.service;

import com.youthexpedition.azit.modules.notification.application.port.in.DispatchPushUseCase;
import com.youthexpedition.azit.modules.notification.application.port.in.command.PushDispatchCommand;
import com.youthexpedition.azit.modules.notification.application.port.out.LoadDeviceTokenPort;
import com.youthexpedition.azit.modules.notification.application.port.out.PushMessage;
import com.youthexpedition.azit.modules.notification.application.port.out.PushSendResult;
import com.youthexpedition.azit.modules.notification.application.port.out.PushSenderPort;
import com.youthexpedition.azit.modules.notification.application.port.out.SaveDeviceTokenPort;
import com.youthexpedition.azit.modules.notification.domain.model.DeviceToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationDispatcher implements DispatchPushUseCase {

    private final LoadDeviceTokenPort loadDeviceTokenPort;
    private final SaveDeviceTokenPort saveDeviceTokenPort;
    private final PushSenderPort pushSenderPort;

    private static final int MULTICAST_CHUNK_SIZE = 500; // FCM 멀티캐스트 1회 최대 토큰 수

    // 트랜잭션을 열지 않음. FCM 왕복 동안 DB 커넥션을 붙잡으면 단일 인스턴스에서 요청 처리와 커넥션을 두고 경합하게 됨
    @Override
    public void dispatch(PushDispatchCommand command) {
        List<String> tokens = loadDeviceTokenPort.findAllByMemberIds(command.receiverIds()).stream()
                .map(DeviceToken::getToken)
                .toList();

        if (tokens.isEmpty()) {
            log.debug("[NOTIFICATION] 등록된 기기가 없어 푸시를 건너뜁니다. receiverIds: {}", command.receiverIds());
            return;
        }

        List<String> invalidTokens = new ArrayList<>();
        for (int start = 0; start < tokens.size(); start += MULTICAST_CHUNK_SIZE) {
            List<String> chunk = tokens.subList(start, Math.min(start + MULTICAST_CHUNK_SIZE, tokens.size()));
            invalidTokens.addAll(sendChunk(command, chunk));
        }

        // 앱 삭제·재설치 등으로 무효해진 토큰은 지움 (쌓이면 발송 지연으로 이어짐)
        if (!invalidTokens.isEmpty()) {
            saveDeviceTokenPort.deleteAllByTokens(invalidTokens);
            log.info("[NOTIFICATION] 무효한 기기 토큰 {}건을 삭제했습니다.", invalidTokens.size());
        }
    }

    private List<String> sendChunk(PushDispatchCommand command, List<String> tokens) {
        try {
            PushSendResult result = pushSenderPort.send(PushMessage.of(
                    tokens, command.title(), command.body(), command.type(), command.crewId(), command.badgeCount()));

            log.info("[NOTIFICATION] 푸시 발송 결과 - 성공: {}건, 실패: {}건", result.successCount(), result.failureCount());
            return result.invalidTokens();
        } catch (Exception e) {
            log.error("[NOTIFICATION] 푸시 발송에 실패했습니다. type: {}, 대상 토큰 수: {}", command.type(), tokens.size(), e); // 알림 생성에 영향 주지 않기 위해 에러 처리 x
            return List.of();
        }
    }
}
