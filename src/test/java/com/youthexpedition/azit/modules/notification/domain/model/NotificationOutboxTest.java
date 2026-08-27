package com.youthexpedition.azit.modules.notification.domain.model;

import com.youthexpedition.azit.modules.notification.domain.model.enums.NotificationOutboxStatus;
import com.youthexpedition.azit.modules.notification.domain.model.enums.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationOutbox 도메인 단위 테스트")
class NotificationOutboxTest {

    private static final int MAX_RETRY_COUNT = 3;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 19, 10, 0);

    @Test
    @DisplayName("성공: 생성 직후에는 처리 대기 상태다.")
    void create_success_pending() {
        // when
        NotificationOutbox outbox = NotificationOutbox.create(NotificationType.CREW_JOIN_REQUESTED, "{}");

        // then
        assertThat(outbox.isPending()).isTrue();
        assertThat(outbox.getRetryCount()).isZero();
    }

    @Test
    @DisplayName("성공: 처리 완료되면 오류 메시지가 비워진다.")
    void markDone_success_clearsErrorMessage() {
        // given
        NotificationOutbox outbox = NotificationOutbox.create(NotificationType.CREW_JOIN_REQUESTED, "{}");
        outbox.markFailed(NOW, "일시적 오류", MAX_RETRY_COUNT);

        // when
        outbox.markDone(NOW);

        // then
        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.DONE);
        assertThat(outbox.getErrorMessage()).isNull();
        assertThat(outbox.getProcessedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("성공: 재시도 한도 이전에는 다시 대기 상태로 남는다.")
    void markFailed_keepsPending_beforeMaxRetryCount() {
        // given
        NotificationOutbox outbox = NotificationOutbox.create(NotificationType.CREW_JOIN_REQUESTED, "{}");

        // when
        outbox.markFailed(NOW, "일시적 오류", MAX_RETRY_COUNT);

        // then
        assertThat(outbox.isPending()).isTrue();
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        assertThat(outbox.getErrorMessage()).isEqualTo("일시적 오류");
    }

    @Test
    @DisplayName("성공: 재시도 한도에 도달하면 FAILED 로 바뀌어 다시 선점되지 않는다.")
    void markFailed_becomesFailed_whenMaxRetryCountReached() {
        // given
        NotificationOutbox outbox = NotificationOutbox.create(NotificationType.CREW_JOIN_REQUESTED, "{}");

        // when
        for (int i = 0; i < MAX_RETRY_COUNT; i++) {
            outbox.markFailed(NOW, "계속 실패", MAX_RETRY_COUNT);
        }

        // then
        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.FAILED);
        assertThat(outbox.isPending()).isFalse();
    }

    @Test
    @DisplayName("성공: 오류 메시지가 길면 잘라서 저장한다.")
    void markFailed_truncatesLongErrorMessage() {
        // given
        NotificationOutbox outbox = NotificationOutbox.create(NotificationType.CREW_JOIN_REQUESTED, "{}");

        // when
        outbox.markFailed(NOW, "e".repeat(600), MAX_RETRY_COUNT);

        // then
        assertThat(outbox.getErrorMessage()).hasSize(500);
    }
}
