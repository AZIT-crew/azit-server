package com.youthexpedition.azit.modules.notification.application.service;

import com.youthexpedition.azit.modules.notification.application.port.in.command.PushDispatchCommand;
import com.youthexpedition.azit.modules.notification.application.port.out.LoadDeviceTokenPort;
import com.youthexpedition.azit.modules.notification.application.port.out.PushMessage;
import com.youthexpedition.azit.modules.notification.application.port.out.PushSendResult;
import com.youthexpedition.azit.modules.notification.application.port.out.PushSenderPort;
import com.youthexpedition.azit.modules.notification.application.port.out.SaveDeviceTokenPort;
import com.youthexpedition.azit.modules.notification.domain.model.DeviceToken;
import com.youthexpedition.azit.modules.notification.domain.model.enums.DeviceType;
import com.youthexpedition.azit.modules.notification.domain.model.enums.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PushNotificationDispatcher 단위 테스트")
class PushNotificationDispatcherTest {

    private static final Long RECEIVER_ID = 1L;
    private static final Long CREW_ID = 10L;

    @Mock
    private LoadDeviceTokenPort loadDeviceTokenPort;
    @Mock
    private SaveDeviceTokenPort saveDeviceTokenPort;
    @Mock
    private PushSenderPort pushSenderPort;

    @InjectMocks
    private PushNotificationDispatcher pushNotificationDispatcher;

    @Test
    @DisplayName("성공 - 등록된 기기가 없으면 발송하지 않는다")
    void dispatch_skips_whenNoDeviceToken() {
        // given
        doReturn(List.of()).when(loadDeviceTokenPort).findAllByMemberIds(List.of(RECEIVER_ID));

        // when
        pushNotificationDispatcher.dispatch(command());

        // then
        verify(pushSenderPort, never()).send(any(PushMessage.class));
    }

    @Test
    @DisplayName("성공 - 무효한 토큰은 삭제한다")
    void dispatch_deletesInvalidTokens() {
        // given
        doReturn(List.of(deviceToken("token-1"), deviceToken("token-2")))
                .when(loadDeviceTokenPort).findAllByMemberIds(List.of(RECEIVER_ID));
        doReturn(PushSendResult.of(1, 1, List.of("token-2"))).when(pushSenderPort).send(any(PushMessage.class));

        // when
        pushNotificationDispatcher.dispatch(command());

        // then
        verify(saveDeviceTokenPort, times(1)).deleteAllByTokens(List.of("token-2"));
    }

    @Test
    @DisplayName("성공 - 토큰이 500개를 넘으면 나눠서 발송한다")
    void dispatch_chunksTokens_whenOverMulticastLimit() {
        // given - FCM 멀티캐스트 1회 상한(500)을 넘는 토큰
        List<DeviceToken> tokens = IntStream.range(0, 501)
                .mapToObj(index -> deviceToken("token-" + index))
                .toList();
        doReturn(tokens).when(loadDeviceTokenPort).findAllByMemberIds(List.of(RECEIVER_ID));
        doReturn(PushSendResult.of(1, 0, List.of())).when(pushSenderPort).send(any(PushMessage.class));

        // when
        pushNotificationDispatcher.dispatch(command());

        // then
        verify(pushSenderPort, times(1)).send(argThat(message -> message.tokens().size() == 500));
        verify(pushSenderPort, times(1)).send(argThat(message -> message.tokens().size() == 1));
    }

    @Test
    @DisplayName("성공 - 발송 중 예외가 나도 밖으로 전파하지 않는다")
    void dispatch_doesNotPropagate_whenSendFails() {
        // given - 푸시 실패가 알림 생성에 영향을 주면 안 된다
        doReturn(List.of(deviceToken("token-1"))).when(loadDeviceTokenPort).findAllByMemberIds(List.of(RECEIVER_ID));
        doThrow(new RuntimeException("FCM 장애")).when(pushSenderPort).send(any(PushMessage.class));

        // when & then
        pushNotificationDispatcher.dispatch(command());

        verify(saveDeviceTokenPort, never()).deleteAllByTokens(anyList());
    }

    private PushDispatchCommand command() {
        return PushDispatchCommand.of(List.of(RECEIVER_ID), NotificationType.CREW_JOIN_APPROVED,
                "크루 가입 승인", "아지트 크루 가입이 승인되었어요!", CREW_ID, 3);
    }

    private DeviceToken deviceToken(String token) {
        return DeviceToken.register(RECEIVER_ID, token, DeviceType.IOS);
    }
}
