package com.youthexpedition.azit.modules.member.domain.model;

import com.youthexpedition.azit.infrastructure.exception.BusinessException;
import com.youthexpedition.azit.modules.member.domain.model.enums.MemberErrorCode;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SocialAccounts 도메인 단위 테스트")
class SocialAccountsTest {

    private MemberSocialAccount account(Long id, SocialProvider provider) {
        return MemberSocialAccount.builder()
                .id(id)
                .memberId(1L)
                .socialProvider(provider)
                .socialProviderId("providerId" + id)
                .build();
    }

    private final MemberSocialAccount kakaoAccount = account(1L, SocialProvider.KAKAO);
    private final MemberSocialAccount appleAccount = account(2L, SocialProvider.APPLE);

    @Test
    @DisplayName("성공: 연동이 2개 이상이면 해제 가능 상태다.")
    void isUnlinkable_true_whenMultipleProvidersLinked() {
        // given
        SocialAccounts socialAccounts = SocialAccounts.of(List.of(kakaoAccount, appleAccount));

        // when & then
        assertThat(socialAccounts.isUnlinkable()).isTrue();
    }

    @Test
    @DisplayName("성공: 연동이 1개뿐이면 해제 불가 상태다.")
    void isUnlinkable_false_whenOnlyOneProviderLinked() {
        // given
        SocialAccounts socialAccounts = SocialAccounts.of(List.of(kakaoAccount));

        // when & then
        assertThat(socialAccounts.isUnlinkable()).isFalse();
    }

    @Test
    @DisplayName("성공: 해제 대상 소셜 계정을 반환한다.")
    void unlink_success_returnsTargetAccount() {
        // given
        SocialAccounts socialAccounts = SocialAccounts.of(List.of(kakaoAccount, appleAccount));

        // when
        MemberSocialAccount target = socialAccounts.unlink(SocialProvider.APPLE);

        // then
        assertThat(target).isEqualTo(appleAccount);
    }

    @Test
    @DisplayName("실패: 마지막 하나 남은 연동은 해제할 수 없다. (계정 미아 방지)")
    void unlink_throwsException_whenLastProvider() {
        // given
        SocialAccounts socialAccounts = SocialAccounts.of(List.of(kakaoAccount));

        // when & then
        assertThatThrownBy(() -> socialAccounts.unlink(SocialProvider.KAKAO))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.CANNOT_UNLINK_LAST_PROVIDER);
    }

    @Test
    @DisplayName("실패: 연동되지 않은 플랫폼은 해제할 수 없다.")
    void unlink_throwsException_whenProviderNotLinked() {
        // given
        SocialAccounts onlyKakao = SocialAccounts.of(List.of(kakaoAccount));

        // when & then - 해제 가능 여부(CANNOT_UNLINK_LAST_PROVIDER)보다 대상 존재 여부를 먼저 검증한다
        assertThatThrownBy(() -> onlyKakao.unlink(SocialProvider.APPLE))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.PROVIDER_NOT_LINKED);
    }

    @Test
    @DisplayName("성공: 연동된 플랫폼 보유 여부를 판단한다.")
    void hasProvider_reflectsLinkedProviders() {
        // given
        SocialAccounts socialAccounts = SocialAccounts.of(List.of(kakaoAccount));

        // when & then
        assertThat(socialAccounts.hasProvider(SocialProvider.KAKAO)).isTrue();
        assertThat(socialAccounts.hasProvider(SocialProvider.APPLE)).isFalse();
    }

    @Test
    @DisplayName("성공: 연동된 계정이 없어도 안전하게 생성된다.")
    void of_success_whenEmpty() {
        // given
        SocialAccounts socialAccounts = SocialAccounts.of(List.of());

        // when & then
        assertThat(socialAccounts.getAccounts()).isEmpty();
        assertThat(socialAccounts.isUnlinkable()).isFalse();
    }
}
