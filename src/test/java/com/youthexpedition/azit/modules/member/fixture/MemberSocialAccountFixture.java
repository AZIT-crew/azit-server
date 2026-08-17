package com.youthexpedition.azit.modules.member.fixture;

import com.youthexpedition.azit.modules.member.domain.model.MemberSocialAccount;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;

import java.time.LocalDateTime;

/**
 * 소셜 연동 테스트에서 반복 사용되는 MemberSocialAccount 픽스처.
 */
public class MemberSocialAccountFixture {

    public static final String KAKAO_PROVIDER_ID = "kakaoId";
    public static final String APPLE_PROVIDER_ID = "appleSub";
    public static final String APPLE_REFRESH_TOKEN = "appleRefreshToken";

    public static MemberSocialAccount kakaoAccount(Long id, Long memberId) {
        return account(id, memberId, SocialProvider.KAKAO, KAKAO_PROVIDER_ID, null);
    }

    public static MemberSocialAccount appleAccount(Long id, Long memberId) {
        return account(id, memberId, SocialProvider.APPLE, APPLE_PROVIDER_ID, APPLE_REFRESH_TOKEN);
    }

    public static MemberSocialAccount account(Long id, Long memberId, SocialProvider socialProvider, String socialProviderId) {
        // 애플만 연동 해제를 위해 리프레시 토큰을 보관한다
        String appleRefreshToken = socialProvider == SocialProvider.APPLE ? APPLE_REFRESH_TOKEN : null;
        return account(id, memberId, socialProvider, socialProviderId, appleRefreshToken);
    }

    public static MemberSocialAccount account(Long id, Long memberId, SocialProvider socialProvider,
                                              String socialProviderId, String appleRefreshToken) {
        return MemberSocialAccount.builder()
                .id(id)
                .memberId(memberId)
                .socialProvider(socialProvider)
                .socialProviderId(socialProviderId)
                .appleRefreshToken(appleRefreshToken)
                .linkedAt(LocalDateTime.now())
                .build();
    }
}
