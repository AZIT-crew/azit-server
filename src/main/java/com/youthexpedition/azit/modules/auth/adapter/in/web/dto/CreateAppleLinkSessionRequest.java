package com.youthexpedition.azit.modules.auth.adapter.in.web.dto;

import com.youthexpedition.azit.modules.auth.application.port.in.command.CreateAppleLinkSessionCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CreateAppleLinkSessionRequest(
        @NotBlank
        @Schema(description = "연동 완료 후 돌아갈 프론트 주소 (허용된 origin만 가능)", example = "https://azitcrew.com/settings/accounts")
        String redirectUrl
) {
    public CreateAppleLinkSessionCommand toCommand(Long memberId) {
        return CreateAppleLinkSessionCommand.of(memberId, redirectUrl);
    }
}
