package com.youthexpedition.azit.modules.notification.domain.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationType {

    CREW_JOIN_REQUESTED("크루 가입 요청", "새로운 크루원이 가입을 요청했어요. 지금 확인해 보세요!"),
    CREW_JOIN_APPROVED("크루 가입 승인", "%s 크루 가입이 승인되었어요!"),
    CREW_JOIN_REJECTED("크루 가입 거절", "%s 크루 가입이 거절되었어요.");

    private final String title;
    private final String bodyTemplate;

    // 크루명을 쓰지 않는 문구는 인자를 무시함
    public String formatBody(String crewName) {
        return String.format(bodyTemplate, crewName);
    }
}
