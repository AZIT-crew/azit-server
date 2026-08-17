package com.youthexpedition.azit.modules.auth.adapter.in.web.dto;

import com.youthexpedition.azit.modules.auth.application.port.in.command.SocialLoginCommand;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import io.swagger.v3.oas.annotations.media.Schema;

public record LinkSocialAccountRequest(
        @Schema(description = "소셜 서비스로부터 발급받은 인가 코드")
        String authorizationCode,

        @Schema(description = "카카오 네이티브 SDK로부터 발급받은 액세스 토큰 (카카오 네이티브 SDK)")
        String accessToken,

        @Schema(description = "애플로부터 발급받은 ID 토큰 (애플 전용)")
        String idToken
) {
    public SocialLoginCommand toCommand(SocialProvider provider) {
        // 연동은 기존 회원에 소셜 계정만 추가하므로, 애플의 최초 가입용 사용자 정보(user)는 사용하지 않는다
        if (provider == SocialProvider.APPLE) {
            return SocialLoginCommand.of(provider, authorizationCode, idToken, null);
        }
        if (accessToken != null && !accessToken.isBlank()) {
            return SocialLoginCommand.ofKakaoNative(provider, accessToken);
        }
        return SocialLoginCommand.of(provider, authorizationCode);
    }
}
