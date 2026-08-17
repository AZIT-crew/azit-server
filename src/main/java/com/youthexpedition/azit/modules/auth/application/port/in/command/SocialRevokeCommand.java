package com.youthexpedition.azit.modules.auth.application.port.in.command;

import com.youthexpedition.azit.modules.member.domain.model.MemberSocialAccount;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;

public record SocialRevokeCommand(
        SocialProvider provider,
        String socialProviderId,
        String refreshToken // 애플용
) {
    public static SocialRevokeCommand from(MemberSocialAccount socialAccount) {
        return new SocialRevokeCommand(
                socialAccount.getSocialProvider(),
                socialAccount.getSocialProviderId(),
                socialAccount.getAppleRefreshToken()
        );
    }
}
