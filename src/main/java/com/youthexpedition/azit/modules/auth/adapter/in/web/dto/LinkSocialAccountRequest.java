package com.youthexpedition.azit.modules.auth.adapter.in.web.dto;

import com.youthexpedition.azit.infrastructure.exception.BusinessException;
import com.youthexpedition.azit.modules.auth.application.port.in.command.SocialLoginCommand;
import com.youthexpedition.azit.modules.auth.domain.model.enums.AuthErrorCode;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import io.swagger.v3.oas.annotations.media.Schema;

public record LinkSocialAccountRequest(
        @Schema(description = "소셜 서비스로부터 발급받은 인가 코드")
        String authorizationCode,

        @Schema(description = "카카오 네이티브 SDK로부터 발급받은 액세스 토큰 (카카오 네이티브 SDK)")
        String accessToken,

        @Schema(description = "사용하지 않습니다. 애플 연동은 연동 세션 발급 API를 통해 진행합니다.", deprecated = true)
        String idToken
) {
    public SocialLoginCommand toCommand(SocialProvider provider) {
        // 애플은 인가 코드가 우리 서버 redirect_uri로 직접 전달되므로 클라이언트가 code를 중계할 수 없다.
        // 연동 세션(state)을 발급받아 애플 인증 화면을 거치는 전용 흐름을 사용해야 한다.
        if (provider == SocialProvider.APPLE) {
            throw new BusinessException(AuthErrorCode.APPLE_LINK_REQUIRES_LINK_SESSION);
        }
        if (accessToken != null && !accessToken.isBlank()) {
            return SocialLoginCommand.ofKakaoNative(provider, accessToken);
        }
        return SocialLoginCommand.of(provider, authorizationCode);
    }
}
