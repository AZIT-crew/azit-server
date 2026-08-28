package com.youthexpedition.azit.modules.notification.application.service;

import com.youthexpedition.azit.modules.notification.application.port.in.command.PushDispatchCommand;
import com.youthexpedition.azit.modules.notification.application.service.dto.OutboxProcessResult;
import com.youthexpedition.azit.modules.notification.domain.model.enums.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationOutboxService 단위 테스트")
class NotificationOutboxServiceTest {

    private static final int CLAIM_SIZE = 3;

    @Mock
    private NotificationOutboxProcessor notificationOutboxProcessor;

    @InjectMocks
    private NotificationOutboxService notificationOutboxService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationOutboxService, "claimSize", CLAIM_SIZE);
    }

    @Test
    @DisplayName("성공 - 처리할 건이 없으면 즉시 사이클을 끝낸다")
    void processPending_stops_whenNothingToProcess() {
        // given
        doReturn(OutboxProcessResult.nothingToProcess()).when(notificationOutboxProcessor).processNext();

        // when
        List<PushDispatchCommand> dispatches = notificationOutboxService.processPending();

        // then
        assertThat(dispatches).isEmpty();
        verify(notificationOutboxProcessor, times(1)).processNext();
    }

    @Test
    @DisplayName("성공 - 처리할 건이 없어질 때까지 이어서 처리한다")
    void processPending_processesUntilEmpty() {
        // given - 2건 처리 후 소진
        doReturn(OutboxProcessResult.processed(pushTarget()),
                OutboxProcessResult.processed(pushTarget()),
                OutboxProcessResult.nothingToProcess())
                .when(notificationOutboxProcessor).processNext();

        // when
        List<PushDispatchCommand> dispatches = notificationOutboxService.processPending();

        // then
        assertThat(dispatches).hasSize(2);
        verify(notificationOutboxProcessor, times(3)).processNext();
    }

    @Test
    @DisplayName("성공 - 한 사이클에서 claim-size 를 넘겨 처리하지 않는다")
    void processPending_stopsAtClaimSize() {
        // given - 처리할 건이 계속 남아 있는 상황
        doReturn(OutboxProcessResult.processed(pushTarget())).when(notificationOutboxProcessor).processNext();

        // when
        List<PushDispatchCommand> dispatches = notificationOutboxService.processPending();

        // then - 남은 건은 다음 사이클로 넘긴다
        assertThat(dispatches).hasSize(CLAIM_SIZE);
        verify(notificationOutboxProcessor, times(CLAIM_SIZE)).processNext();
    }

    @Test
    @DisplayName("성공 - 푸시 대상이 없는 건도 처리 건수에 포함해 사이클이 멈추지 않는다")
    void processPending_continues_whenProcessedWithoutPushTarget() {
        // given - 전체 알림이 꺼졌거나 처리에 실패한 건
        doReturn(OutboxProcessResult.processed(null),
                OutboxProcessResult.processed(pushTarget()),
                OutboxProcessResult.nothingToProcess())
                .when(notificationOutboxProcessor).processNext();

        // when
        List<PushDispatchCommand> dispatches = notificationOutboxService.processPending();

        // then
        assertThat(dispatches).hasSize(1);
        verify(notificationOutboxProcessor, times(3)).processNext();
    }

    private PushDispatchCommand pushTarget() {
        return PushDispatchCommand.of(List.of(1L), NotificationType.CREW_JOIN_APPROVED,
                "크루 가입 승인", "아지트 크루 가입이 승인되었어요!", 10L, 1);
    }
}
