package com.youthexpedition.azit.modules.notification.application.service;

import com.youthexpedition.azit.modules.notification.application.port.in.command.RegisterDeviceTokenCommand;
import com.youthexpedition.azit.modules.notification.application.port.out.LoadDeviceTokenPort;
import com.youthexpedition.azit.modules.notification.application.port.out.SaveDeviceTokenPort;
import com.youthexpedition.azit.modules.notification.domain.model.DeviceToken;
import com.youthexpedition.azit.modules.notification.domain.model.enums.DeviceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceTokenService 단위 테스트")
class DeviceTokenServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final String TOKEN = "fcm-token";

    @Mock
    private LoadDeviceTokenPort loadDeviceTokenPort;
    @Mock
    private SaveDeviceTokenPort saveDeviceTokenPort;

    @InjectMocks
    private DeviceTokenService deviceTokenService;

    @Test
    @DisplayName("성공 - 등록된 적 없는 토큰은 새로 저장된다")
    void register_success_savesNewToken() {
        // given
        doReturn(Optional.empty()).when(loadDeviceTokenPort).findByToken(TOKEN);

        // when
        deviceTokenService.register(MEMBER_ID, RegisterDeviceTokenCommand.of(TOKEN, DeviceType.IOS));

        // then
        verify(saveDeviceTokenPort, times(1)).save(argThat(deviceToken ->
                deviceToken.isOwnedBy(MEMBER_ID) && deviceToken.getToken().equals(TOKEN)
        ));
    }

    @Test
    @DisplayName("성공 - 다른 계정이 쓰던 기기면 소유자만 바뀐다")
    void register_success_changesOwner_whenTokenBelongsToAnotherMember() {
        // given
        DeviceToken existing = DeviceToken.register(2L, TOKEN, DeviceType.ANDROID);
        doReturn(Optional.of(existing)).when(loadDeviceTokenPort).findByToken(TOKEN);

        // when
        deviceTokenService.register(MEMBER_ID, RegisterDeviceTokenCommand.of(TOKEN, DeviceType.IOS));

        // then - 이전 계정으로 알림이 가지 않아야 한다
        verify(saveDeviceTokenPort, times(1)).save(argThat(deviceToken ->
                deviceToken.isOwnedBy(MEMBER_ID) && deviceToken.getDeviceType() == DeviceType.IOS
        ));
    }

    @Test
    @DisplayName("성공 - 본인 토큰이면 삭제한다")
    void delete_success_whenOwnedByMember() {
        // given
        doReturn(Optional.of(DeviceToken.register(MEMBER_ID, TOKEN, DeviceType.IOS)))
                .when(loadDeviceTokenPort).findByToken(TOKEN);

        // when
        deviceTokenService.delete(MEMBER_ID, TOKEN);

        // then
        verify(saveDeviceTokenPort, times(1)).deleteByToken(TOKEN);
    }

    @Test
    @DisplayName("성공 - 다른 회원의 토큰이면 삭제하지 않고 조용히 넘어간다")
    void delete_skips_whenTokenBelongsToAnotherMember() {
        // given
        doReturn(Optional.of(DeviceToken.register(2L, TOKEN, DeviceType.IOS)))
                .when(loadDeviceTokenPort).findByToken(TOKEN);

        // when
        deviceTokenService.delete(MEMBER_ID, TOKEN);

        // then - 로그아웃 흐름을 막지 않는다
        verify(saveDeviceTokenPort, never()).deleteByToken(anyString());
    }

    @Test
    @DisplayName("성공 - 등록되지 않은 토큰이면 아무 일도 하지 않는다")
    void delete_skips_whenTokenNotFound() {
        // given
        doReturn(Optional.empty()).when(loadDeviceTokenPort).findByToken(TOKEN);

        // when
        deviceTokenService.delete(MEMBER_ID, TOKEN);

        // then
        verify(saveDeviceTokenPort, never()).deleteByToken(anyString());
    }
}
