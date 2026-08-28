package com.youthexpedition.azit.modules.notification.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youthexpedition.azit.modules.crew.application.port.out.LoadCrewMemberPort;
import com.youthexpedition.azit.modules.crew.application.port.out.LoadCrewPort;
import com.youthexpedition.azit.modules.crew.domain.model.Crew;
import com.youthexpedition.azit.modules.crew.domain.model.CrewMember;
import com.youthexpedition.azit.modules.crew.domain.model.enums.CrewMemberRole;
import com.youthexpedition.azit.modules.crew.domain.model.enums.CrewMemberStatus;
import com.youthexpedition.azit.modules.member.application.port.out.LoadMemberPort;
import com.youthexpedition.azit.modules.member.domain.model.Member;
import com.youthexpedition.azit.modules.member.fixture.MemberFixture;
import com.youthexpedition.azit.modules.notification.application.port.out.LoadNotificationOutboxPort;
import com.youthexpedition.azit.modules.notification.application.port.out.SaveNotificationOutboxPort;
import com.youthexpedition.azit.modules.notification.application.port.out.LoadNotificationPort;
import com.youthexpedition.azit.modules.notification.application.port.out.SaveNotificationPort;
import com.youthexpedition.azit.modules.notification.application.service.dto.OutboxProcessResult;
import com.youthexpedition.azit.modules.notification.domain.model.NotificationOutbox;
import com.youthexpedition.azit.modules.notification.domain.model.enums.NotificationOutboxStatus;
import com.youthexpedition.azit.modules.notification.domain.model.enums.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationOutboxProcessor 단위 테스트")
class NotificationOutboxProcessorTest {

    private static final Long CREW_ID = 10L;
    private static final Long LEADER_ID = 1L;
    private static final Long REQUESTER_ID = 2L;
    private static final int MAX_RETRY_COUNT = 3;

    @Mock
    private LoadNotificationOutboxPort loadNotificationOutboxPort;
    @Mock
    private SaveNotificationOutboxPort saveNotificationOutboxPort;
    @Mock
    private SaveNotificationPort saveNotificationPort;
    @Mock
    private LoadNotificationPort loadNotificationPort;
    @Mock
    private LoadCrewPort loadCrewPort;
    @Mock
    private LoadCrewMemberPort loadCrewMemberPort;
    @Mock
    private LoadMemberPort loadMemberPort;

    @InjectMocks
    private NotificationOutboxProcessor notificationOutboxProcessor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationOutboxProcessor, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(notificationOutboxProcessor, "maxRetryCount", MAX_RETRY_COUNT);
        ReflectionTestUtils.setField(notificationOutboxProcessor, "retryBackoffSeconds", 60);
    }

    @Test
    @DisplayName("성공 - 처리할 아웃박스가 없으면 처리하지 않았음을 알린다")
    void processNext_returnsNothingToProcess_whenNoOutboxClaimed() {
        // given
        doReturn(Optional.empty()).when(loadNotificationOutboxPort).claimNext(any(LocalDateTime.class));

        // when
        OutboxProcessResult result = notificationOutboxProcessor.processNext();

        // then
        assertThat(result.processed()).isFalse();
        verify(saveNotificationPort, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("성공 - 가입 요청 알림은 그 시점의 크루 리더에게 생성된다")
    void processNext_success_sendsJoinRequestToLeader() {
        // given
        NotificationOutbox outbox = outbox(NotificationType.CREW_JOIN_REQUESTED, REQUESTER_ID, CREW_ID);
        doReturn(Optional.of(outbox)).when(loadNotificationOutboxPort).claimNext(any(LocalDateTime.class));
        doReturn(Optional.of(crew())).when(loadCrewPort).findById(CREW_ID);
        doReturn(Optional.of(leader())).when(loadCrewMemberPort).findLeaderByCrewId(CREW_ID);
        doReturn(Optional.of(notificationAgreedMember(LEADER_ID))).when(loadMemberPort).findById(LEADER_ID);
        doReturn(3L).when(loadNotificationPort).countUnreadByReceiverId(LEADER_ID);

        // when
        OutboxProcessResult result = notificationOutboxProcessor.processNext();

        // then
        verify(saveNotificationPort, times(1)).saveAll(argThat(notifications ->
                notifications.size() == 1 && notifications.get(0).getReceiverId().equals(LEADER_ID)
        ));
        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.DONE);
        assertThat(result.hasPushTarget()).isTrue();
        assertThat(result.pushTarget().receiverIds()).containsExactly(LEADER_ID);
        assertThat(result.pushTarget().badgeCount()).isEqualTo(3); // iOS 배지에 실을 안 읽은 알림 개수
    }

    @Test
    @DisplayName("성공 - 승인 알림은 리더 조회 없이 신청자에게 생성된다")
    void processNext_success_sendsApprovalToRequester() {
        // given
        doReturn(Optional.of(outbox(NotificationType.CREW_JOIN_APPROVED, REQUESTER_ID, CREW_ID)))
                .when(loadNotificationOutboxPort).claimNext(any(LocalDateTime.class));
        doReturn(Optional.of(crew())).when(loadCrewPort).findById(CREW_ID);
        doReturn(Optional.of(notificationAgreedMember(REQUESTER_ID))).when(loadMemberPort).findById(REQUESTER_ID);

        // when
        notificationOutboxProcessor.processNext();

        // then
        verify(loadCrewMemberPort, never()).findLeaderByCrewId(anyLong());
        verify(saveNotificationPort, times(1)).saveAll(argThat(notifications ->
                notifications.get(0).getReceiverId().equals(REQUESTER_ID)
                        && notifications.get(0).getBody().contains("아지트")
        ));
    }

    @Test
    @DisplayName("성공 - 전체 알림이 꺼진 회원도 인앱 알림은 저장하되 푸시 대상에서는 빠진다")
    void processNext_savesNotificationButSkipsPush_whenNotificationDisagreed() {
        // given - 알림 수신에 동의하지 않은 회원
        doReturn(Optional.of(outbox(NotificationType.CREW_JOIN_APPROVED, REQUESTER_ID, CREW_ID)))
                .when(loadNotificationOutboxPort).claimNext(any(LocalDateTime.class));
        doReturn(Optional.of(crew())).when(loadCrewPort).findById(CREW_ID);
        doReturn(Optional.of(MemberFixture.activeMember(REQUESTER_ID))).when(loadMemberPort).findById(REQUESTER_ID);

        // when
        OutboxProcessResult result = notificationOutboxProcessor.processNext();

        // then
        verify(saveNotificationPort, times(1)).saveAll(anyList());
        assertThat(result.processed()).isTrue();
        assertThat(result.hasPushTarget()).isFalse();
    }

    @Test
    @DisplayName("성공 - 처리에 실패하면 실패 횟수가 쌓이고 재시도 대상으로 남는다")
    void processNext_marksFailed_whenProcessingFails() {
        // given - 크루를 찾을 수 없는 아웃박스
        NotificationOutbox outbox = outbox(NotificationType.CREW_JOIN_APPROVED, REQUESTER_ID, 999L);
        doReturn(Optional.of(outbox)).when(loadNotificationOutboxPort).claimNext(any(LocalDateTime.class));
        doReturn(Optional.empty()).when(loadCrewPort).findById(999L);

        // when
        OutboxProcessResult result = notificationOutboxProcessor.processNext();

        // then - 실패도 이번 사이클에서 처리한 건으로 세어 루프가 멈추지 않게 한다
        assertThat(result.processed()).isTrue();
        assertThat(result.hasPushTarget()).isFalse();
        assertThat(outbox.isPending()).isTrue();
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        verify(saveNotificationOutboxPort, times(1)).save(outbox); // 실패 기록이 커밋되어야 재시도 횟수가 누적된다
        verify(saveNotificationPort, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("성공 - 재시도 한도를 넘긴 아웃박스는 FAILED 로 바뀌어 다시 선점되지 않는다")
    void processNext_marksFailedFinally_whenMaxRetryCountReached() {
        // given - 이미 한도 직전까지 실패한 아웃박스
        NotificationOutbox outbox = NotificationOutbox.builder()
                .id(1L)
                .type(NotificationType.CREW_JOIN_APPROVED)
                .payload(payload(999L, REQUESTER_ID))
                .status(NotificationOutboxStatus.PENDING)
                .retryCount(MAX_RETRY_COUNT - 1)
                .build();
        doReturn(Optional.of(outbox)).when(loadNotificationOutboxPort).claimNext(any(LocalDateTime.class));
        doReturn(Optional.empty()).when(loadCrewPort).findById(999L);

        // when
        notificationOutboxProcessor.processNext();

        // then
        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.FAILED);
        assertThat(outbox.isPending()).isFalse();
    }

    private NotificationOutbox outbox(NotificationType type, Long memberId, Long crewId) {
        return NotificationOutbox.builder()
                .id(1L)
                .type(type)
                .payload(payload(crewId, memberId))
                .status(NotificationOutboxStatus.PENDING)
                .retryCount(0)
                .build();
    }

    private String payload(Long crewId, Long memberId) {
        return "{\"crewId\":" + crewId + ",\"memberId\":" + memberId + "}";
    }

    private Crew crew() {
        return Crew.builder().id(CREW_ID).name("아지트").build();
    }

    private CrewMember leader() {
        return CrewMember.builder()
                .crewId(CREW_ID)
                .memberId(LEADER_ID)
                .role(CrewMemberRole.LEADER)
                .status(CrewMemberStatus.JOINED)
                .build();
    }

    private Member notificationAgreedMember(Long memberId) {
        Member member = MemberFixture.activeMember(memberId);
        member.updateNotificationConsent(true, LocalDateTime.now());
        return member;
    }
}
