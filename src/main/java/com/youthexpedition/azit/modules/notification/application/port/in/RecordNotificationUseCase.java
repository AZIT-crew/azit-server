package com.youthexpedition.azit.modules.notification.application.port.in;

/**
 * 알림 발송 요청 기록 (아웃박스).
 * 비즈니스 로직에서 호출하며, 호출자의 트랜잭션 안에서 기록되어야 함.
 */
public interface RecordNotificationUseCase {
    void recordCrewJoinRequested(Long crewId, Long requesterMemberId);
    void recordCrewJoinApproved(Long crewId, Long targetMemberId);
    void recordCrewJoinRejected(Long crewId, Long targetMemberId);
}
