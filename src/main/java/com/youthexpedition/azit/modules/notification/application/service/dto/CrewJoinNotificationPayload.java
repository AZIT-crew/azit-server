package com.youthexpedition.azit.modules.notification.application.service.dto;

public record CrewJoinNotificationPayload(
        Long crewId,
        Long memberId // 요청 알림이면 크루 리더, 승인·거절 알림이면 신청자
) {
    public static CrewJoinNotificationPayload of(Long crewId, Long memberId) {
        return new CrewJoinNotificationPayload(crewId, memberId);
    }
}
