package com.youthexpedition.azit.modules.member.application.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CrewNotificationSettingResponse(
        @Schema(description = "크루 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        Long crewId,
        @Schema(description = "크루 이름", requiredMode = Schema.RequiredMode.REQUIRED)
        String crewName,
        @Schema(description = "크루 이미지 URL", nullable = true, requiredMode = Schema.RequiredMode.REQUIRED)
        String crewImageUrl,
        @Schema(description = "크루 전체알림 토글. 정기런·번개런이 모두 켜져 있을 때만 true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean allEnabled,
        @Schema(description = "정기런 알림", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean regularRunEnabled,
        @Schema(description = "번개런 알림", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean lightningRunEnabled
) {
    public static CrewNotificationSettingResponse of(Long crewId, String crewName, String crewImageUrl,
                                                     boolean regularRunEnabled, boolean lightningRunEnabled) {
        return new CrewNotificationSettingResponse(
                crewId, crewName, crewImageUrl,
                regularRunEnabled && lightningRunEnabled, // 전체알림은 파생값
                regularRunEnabled, lightningRunEnabled);
    }
}
