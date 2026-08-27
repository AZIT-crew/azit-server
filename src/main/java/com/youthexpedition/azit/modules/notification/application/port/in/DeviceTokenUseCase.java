package com.youthexpedition.azit.modules.notification.application.port.in;

import com.youthexpedition.azit.modules.notification.application.port.in.command.RegisterDeviceTokenCommand;

public interface DeviceTokenUseCase {
    void register(Long memberId, RegisterDeviceTokenCommand command);
    void delete(Long memberId, String token);
}
