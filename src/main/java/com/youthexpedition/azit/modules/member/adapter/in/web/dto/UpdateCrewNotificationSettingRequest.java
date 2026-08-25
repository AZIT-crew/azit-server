package com.youthexpedition.azit.modules.member.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.youthexpedition.azit.modules.member.application.port.in.command.UpdateCrewNotificationSettingCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;

public record UpdateCrewNotificationSettingRequest(
        @Schema(description = "크루 전체알림 (지정하면 정기런·번개런을 한 번에 변경, null이면 변경하지 않음)",
                nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Boolean allEnabled,

        @Schema(description = "정기런 알림 여부 (null이면 변경하지 않음)",
                nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Boolean regularRunEnabled,

        @Schema(description = "번개런 알림 여부 (null이면 변경하지 않음)",
                nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Boolean lightningRunEnabled
) {
    // 부분 갱신이므로 개별 항목은 null을 허용하되, 변경할 항목이 하나도 없는 요청은 거부
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "변경할 알림 항목이 없습니다.")
    public boolean isAnyNotificationSpecified() {
        return allEnabled != null || regularRunEnabled != null || lightningRunEnabled != null;
    }

    public UpdateCrewNotificationSettingCommand toCommand() {
        return UpdateCrewNotificationSettingCommand.of(allEnabled, regularRunEnabled, lightningRunEnabled);
    }
}
