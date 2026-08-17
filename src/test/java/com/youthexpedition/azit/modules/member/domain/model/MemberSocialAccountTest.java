package com.youthexpedition.azit.modules.member.domain.model;

import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DisplayName("MemberSocialAccount 도메인 단위 테스트")
class MemberSocialAccountTest {

    private MemberSocialAccount kakaoAccount() {
        return MemberSocialAccount.link(1L, SocialProvider.KAKAO, "12345", "test@kakao.com", true, null);
    }

    private MemberSocialAccount appleAccount() {
        return MemberSocialAccount.link(1L, SocialProvider.APPLE, "appleSub", "test@privaterelay.appleid.com", true, "oldToken");
    }

    @Test
    @DisplayName("성공: 연동 시 연동 시점이 기록된다.")
    void link_success_recordsLinkedAt() {
        // when
        MemberSocialAccount socialAccount = kakaoAccount();

        // then
        assertThat(socialAccount.getMemberId()).isEqualTo(1L);
        assertThat(socialAccount.getSocialProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(socialAccount.getSocialProviderId()).isEqualTo("12345");
        assertThat(socialAccount.getLinkedAt()).isNotNull();
    }

    @Test
    @DisplayName("성공: 애플 계정은 리프레시 토큰이 최신값으로 갱신된다.")
    void updateAppleRefreshToken_success_whenApple() {
        // given
        MemberSocialAccount socialAccount = appleAccount();

        // when
        socialAccount.updateAppleRefreshToken("newToken");

        // then
        assertThat(socialAccount.getAppleRefreshToken()).isEqualTo("newToken");
    }

    @Test
    @DisplayName("성공: 애플이 아닌 계정은 리프레시 토큰이 갱신되지 않는다.")
    void updateAppleRefreshToken_ignored_whenNotApple() {
        // given
        MemberSocialAccount socialAccount = kakaoAccount();

        // when
        socialAccount.updateAppleRefreshToken("newToken");

        // then
        assertThat(socialAccount.getAppleRefreshToken()).isNull();
    }

    @Test
    @DisplayName("성공: 이메일 공유 상태가 계정 단위로 변경된다.")
    void updateEmailSharingStatus_success() {
        // given
        MemberSocialAccount socialAccount = appleAccount();

        // when
        socialAccount.updateEmailSharingStatus(false);

        // then
        assertThat(socialAccount.isEmailSharingEnabled()).isFalse();
    }
}
