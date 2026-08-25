package com.youthexpedition.azit.modules.member.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MemberCrewNotificationSetting 도메인 단위 테스트")
class MemberCrewNotificationSettingTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long CREW_ID = 10L;

    @Test
    @DisplayName("성공: 설정을 변경한 적 없는 크루는 모든 알림이 켜진 상태다.")
    void defaultSetting_success_allEnabled() {
        // when
        MemberCrewNotificationSetting setting = MemberCrewNotificationSetting.defaultSetting(MEMBER_ID, CREW_ID);

        // then
        assertThat(setting.isRegularRunEnabled()).isTrue();
        assertThat(setting.isLightningRunEnabled()).isTrue();
        assertThat(setting.isAllEnabled()).isTrue();
    }

    @Test
    @DisplayName("성공: 전체알림을 끄면 정기런·번개런이 함께 꺼진다.")
    void updateAll_success_disablesBothRunTypes() {
        // given
        MemberCrewNotificationSetting setting = MemberCrewNotificationSetting.defaultSetting(MEMBER_ID, CREW_ID);

        // when
        setting.updateAll(false);

        // then
        assertThat(setting.isRegularRunEnabled()).isFalse();
        assertThat(setting.isLightningRunEnabled()).isFalse();
        assertThat(setting.isAllEnabled()).isFalse();
    }

    @Test
    @DisplayName("성공: 하나만 꺼도 전체알림은 꺼진 것으로 판단한다.")
    void isAllEnabled_returnsFalse_whenOnlyOneRunTypeDisabled() {
        // given
        MemberCrewNotificationSetting setting = MemberCrewNotificationSetting.defaultSetting(MEMBER_ID, CREW_ID);

        // when - 번개런만 끈다
        setting.update(null, false);

        // then
        assertThat(setting.isRegularRunEnabled()).isTrue(); // null인 항목은 유지
        assertThat(setting.isLightningRunEnabled()).isFalse();
        assertThat(setting.isAllEnabled()).isFalse();
    }

    @Test
    @DisplayName("성공: 두 알림을 모두 다시 켜면 전체알림도 켜진 것으로 판단한다.")
    void isAllEnabled_returnsTrue_whenBothRunTypesEnabled() {
        // given
        MemberCrewNotificationSetting setting = MemberCrewNotificationSetting.defaultSetting(MEMBER_ID, CREW_ID);
        setting.updateAll(false);

        // when
        setting.update(true, true);

        // then
        assertThat(setting.isAllEnabled()).isTrue();
    }
}
