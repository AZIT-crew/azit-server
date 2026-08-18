package com.youthexpedition.azit.modules.member.application.port.in.command;

// null인 항목은 변경하지 않는다 (부분 갱신)
public record UpdateOptionalTermsCommand(
        Boolean marketingAgreed,     // 마케팅 정보 수신 동의
        Boolean notificationAgreed   // 알림 수신 동의 (전체 알림)
) {
    public static UpdateOptionalTermsCommand of(Boolean marketingAgreed, Boolean notificationAgreed) {
        return new UpdateOptionalTermsCommand(marketingAgreed, notificationAgreed);
    }
}
