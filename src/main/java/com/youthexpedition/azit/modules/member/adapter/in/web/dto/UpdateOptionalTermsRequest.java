package com.youthexpedition.azit.modules.member.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.youthexpedition.azit.modules.member.application.port.in.command.UpdateOptionalTermsCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;

public record UpdateOptionalTermsRequest(
        @Schema(description = "마케팅 정보 수신 동의 여부 (null이면 변경하지 않음)",
                nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Boolean marketingAgreed,

        @Schema(description = "알림 수신 동의 여부 (null이면 변경하지 않음)",
                nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Boolean notificationAgreed
) {
    // 부분 갱신이므로 개별 항목은 null을 허용하되, 변경할 항목이 하나도 없는 요청은 거부
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "변경할 선택 약관 항목이 없습니다.")
    public boolean isAnyTermsSpecified() {
        return marketingAgreed != null || notificationAgreed != null;
    }

    public UpdateOptionalTermsCommand toCommand() {
        return UpdateOptionalTermsCommand.of(marketingAgreed, notificationAgreed);
    }
}
