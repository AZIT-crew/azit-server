package com.youthexpedition.azit.modules.auth.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CreateAppleLinkSessionRequest(
        @NotBlank
        @Schema(description = "연동 완료 후 리다이렉트 될 프론트 주소")
        String redirectUrl
) {}
