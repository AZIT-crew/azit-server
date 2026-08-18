package com.youthexpedition.azit.modules.member.application.port.in.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record OptionalTermsResponse(
        @Schema(description = "마케팅 정보 수신 동의", requiredMode = Schema.RequiredMode.REQUIRED)
        OptionalTermsItem marketing,
        @Schema(description = "알림 수신 동의", requiredMode = Schema.RequiredMode.REQUIRED)
        OptionalTermsItem notification
) {
    public static OptionalTermsResponse of(OptionalTermsItem marketing, OptionalTermsItem notification) {
        return new OptionalTermsResponse(marketing, notification);
    }

    public record OptionalTermsItem(
            @Schema(description = "동의 여부 (토글 on/off)", requiredMode = Schema.RequiredMode.REQUIRED)
            boolean agreed,
            @Schema(description = "동의 여부를 마지막으로 변경한 일시 (변경 이력이 없으면 null)",
                    nullable = true, requiredMode = Schema.RequiredMode.REQUIRED)
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime changedAt
    ) {
        public static OptionalTermsItem of(boolean agreed, LocalDateTime changedAt) {
            return new OptionalTermsItem(agreed, changedAt);
        }
    }
}
