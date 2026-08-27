package com.youthexpedition.azit.modules.notification.domain.model;

import com.youthexpedition.azit.modules.notification.domain.model.enums.DeviceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeviceToken 도메인 단위 테스트")
class DeviceTokenTest {

    private static final Long MEMBER_ID = 1L;
    private static final String TOKEN = "fcm-token";

    @Test
    @DisplayName("성공: 같은 기기를 다른 계정이 등록하면 소유자가 바뀐다.")
    void changeOwner_success() {
        // given
        DeviceToken deviceToken = DeviceToken.register(MEMBER_ID, TOKEN, DeviceType.IOS);

        // when
        deviceToken.changeOwner(2L, DeviceType.ANDROID);

        // then
        assertThat(deviceToken.getMemberId()).isEqualTo(2L);
        assertThat(deviceToken.getDeviceType()).isEqualTo(DeviceType.ANDROID);
        assertThat(deviceToken.isOwnedBy(MEMBER_ID)).isFalse();
    }
}
