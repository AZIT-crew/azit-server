package com.youthexpedition.azit.modules.notification.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record DeleteDeviceTokenRequest(
        @Schema(description = "삭제할 FCM 기기 토큰", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "기기 토큰은 필수입니다.")
        String token
) {
}
