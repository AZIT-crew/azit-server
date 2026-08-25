package com.youthexpedition.azit.modules.member.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MemberCrewNotificationSetting {
    private final Long id;
    private final Long memberId;
    private final Long crewId;
    private boolean isRegularRunEnabled;
    private boolean isLightningRunEnabled;

    // 설정을 변경한 적이 없는 크루는 모든 알림이 켜진 상태로 세팅
    public static MemberCrewNotificationSetting defaultSetting(Long memberId, Long crewId) {
        return MemberCrewNotificationSetting.builder()
                .memberId(memberId)
                .crewId(crewId)
                .isRegularRunEnabled(true)
                .isLightningRunEnabled(true)
                .build();
    }

    // 크루 '전체알림' 토글 상태
    public boolean isAllEnabled() {
        return this.isRegularRunEnabled && this.isLightningRunEnabled;
    }

    // 크루 '전체알림' 토글 (정기런, 번개런 모두 켜기)
    public void updateAll(boolean enabled) {
        this.isRegularRunEnabled = enabled;
        this.isLightningRunEnabled = enabled;
    }

    // 개별 알림 변경
    public void update(Boolean regularRunEnabled, Boolean lightningRunEnabled) {
        if (regularRunEnabled != null) {
            this.isRegularRunEnabled = regularRunEnabled;
        }
        if (lightningRunEnabled != null) {
            this.isLightningRunEnabled = lightningRunEnabled;
        }
    }
}
