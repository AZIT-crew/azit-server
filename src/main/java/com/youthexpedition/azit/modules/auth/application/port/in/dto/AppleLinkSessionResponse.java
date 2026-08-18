package com.youthexpedition.azit.modules.auth.application.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AppleLinkSessionResponse(
        @Schema(description = "애플 인증 요청의 state 파라미터에 그대로 넣어야 하는 값", example = "9f1c0b7c4e0a4d8fae2f0f1b6f3f7a12")
        String state
) {
    public static AppleLinkSessionResponse of(String state) {
        return new AppleLinkSessionResponse(state);
    }
}
