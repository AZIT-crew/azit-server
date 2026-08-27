package com.youthexpedition.azit.modules.notification.application.port.in.command;

import com.youthexpedition.azit.modules.notification.domain.model.enums.DeviceType;

public record RegisterDeviceTokenCommand(
        String token,
        DeviceType deviceType
) {
    public static RegisterDeviceTokenCommand of(String token, DeviceType deviceType) {
        return new RegisterDeviceTokenCommand(token, deviceType);
    }
}
