package com.youthexpedition.azit.modules.notification.domain.model;

import com.youthexpedition.azit.modules.notification.domain.model.enums.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Notification 도메인 단위 테스트")
class NotificationTest {

    private static final Long RECEIVER_ID = 1L;
    private static final Long CREW_ID = 10L;

    @Test
    @DisplayName("성공: 승인 알림 문구에 크루명이 채워진다.")
    void create_success_fillsCrewNameInBody() {
        // when
        Notification notification = Notification.create(RECEIVER_ID, NotificationType.CREW_JOIN_APPROVED, CREW_ID, "아지트");

        // then
        assertThat(notification.getTitle()).isEqualTo("크루 가입 승인");
        assertThat(notification.getBody()).isEqualTo("아지트 크루 가입이 승인되었어요!");
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    @DisplayName("성공: 크루명을 쓰지 않는 문구는 인자를 무시한다.")
    void create_success_keepsBody_whenTemplateHasNoCrewName() {
        // when
        Notification notification = Notification.create(RECEIVER_ID, NotificationType.CREW_JOIN_REQUESTED, CREW_ID, "아지트");

        // then
        assertThat(notification.getBody()).isEqualTo("새로운 크루원이 가입을 요청했어요. 지금 확인해 보세요!");
    }

    @Test
    @DisplayName("성공: 읽음 처리하면 읽은 시점이 기록된다.")
    void markAsRead_success() {
        // given
        Notification notification = Notification.create(RECEIVER_ID, NotificationType.CREW_JOIN_APPROVED, CREW_ID, "아지트");
        LocalDateTime now = LocalDateTime.of(2026, 8, 19, 10, 0);

        // when
        notification.markAsRead(now);

        // then
        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("성공: 이미 읽은 알림을 다시 읽어도 읽은 시점이 바뀌지 않는다.")
    void markAsRead_keepsReadAt_whenAlreadyRead() {
        // given
        Notification notification = Notification.create(RECEIVER_ID, NotificationType.CREW_JOIN_APPROVED, CREW_ID, "아지트");
        LocalDateTime firstReadAt = LocalDateTime.of(2026, 8, 19, 10, 0);
        notification.markAsRead(firstReadAt);

        // when
        notification.markAsRead(firstReadAt.plusHours(1));

        // then
        assertThat(notification.getReadAt()).isEqualTo(firstReadAt);
    }
}
