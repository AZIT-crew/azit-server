package com.youthexpedition.azit.modules.notification.adapter.in.web.dto;

import com.youthexpedition.azit.modules.notification.application.port.in.command.RegisterDeviceTokenCommand;
import com.youthexpedition.azit.modules.notification.domain.model.enums.DeviceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterDeviceTokenRequest(
        @Schema(description = "FCM 기기 토큰", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "기기 토큰은 필수입니다.")
        String token,

        @Schema(description = "기기 종류 (IOS, ANDROID)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "기기 종류는 필수입니다.")
        DeviceType deviceType
) {
    public RegisterDeviceTokenCommand toCommand() {
        return RegisterDeviceTokenCommand.of(token, deviceType);
    }
}
