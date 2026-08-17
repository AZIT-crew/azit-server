package com.youthexpedition.azit.modules.member.application.port.in.dto;

import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record LinkedProviderResponse(
        @Schema(description = "소셜 플랫폼별 연동 상태")
        List<LinkedProviderItem> providers
) {
    public static LinkedProviderResponse of(List<LinkedProviderItem> providers) {
        return new LinkedProviderResponse(providers);
    }

    public record LinkedProviderItem(
            @Schema(description = "소셜 플랫폼")
            SocialProvider provider,

            @Schema(description = "플랫폼 표시명")
            String providerName,

            @Schema(description = "연동 여부")
            boolean isLinked,

            @Schema(description = "이메일 (미연동이거나 이메일 미제공 시 null)")
            String email,

            @Schema(description = "연동 일자 (미연동 시 null)")
            LocalDate linkedAt,

            @Schema(description = "연동 해제 가능 여부. 연동된 소셜이 1개뿐이면 false")
            boolean isUnlinkable
    ) {
        public static LinkedProviderItem linked(SocialProvider provider, String email,
                                                LocalDate linkedAt, boolean isUnlinkable) {
            return new LinkedProviderItem(provider, provider.getDescription(), true, email, linkedAt, isUnlinkable);
        }

        public static LinkedProviderItem notLinked(SocialProvider provider) {
            return new LinkedProviderItem(provider, provider.getDescription(), false, null, null, false);
        }
    }
}
