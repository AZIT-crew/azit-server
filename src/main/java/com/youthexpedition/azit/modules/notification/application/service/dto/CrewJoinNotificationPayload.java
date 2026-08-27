package com.youthexpedition.azit.modules.notification.application.service.dto;

/**
 * 크루 가입 관련 알림의 아웃박스 payload.
 * 수신자는 폴러가 처리하는 시점에 확정함 (요청 알림은 그때의 크루 리더에게 감).
 */
public record CrewJoinNotificationPayload(
        Long crewId,
        Long memberId // 요청 알림이면 신청자, 승인·거절 알림이면 수신자
) {
    public static CrewJoinNotificationPayload of(Long crewId, Long memberId) {
        return new CrewJoinNotificationPayload(crewId, memberId);
    }
}
