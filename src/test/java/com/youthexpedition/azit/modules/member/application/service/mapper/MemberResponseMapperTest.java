package com.youthexpedition.azit.modules.member.application.service.mapper;

import com.youthexpedition.azit.infrastructure.common.util.image.ImageUrlFormatUtil;
import com.youthexpedition.azit.modules.member.application.port.in.dto.LinkedProviderResponse;
import com.youthexpedition.azit.modules.member.application.port.in.dto.LinkedProviderResponse.LinkedProviderItem;
import com.youthexpedition.azit.modules.member.domain.model.MemberSocialAccount;
import com.youthexpedition.azit.modules.member.domain.model.SocialAccounts;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberResponseMapper 단위 테스트")
class MemberResponseMapperTest {

    @Mock private ImageUrlFormatUtil imageUrlFormatUtil;

    @InjectMocks
    private MemberResponseMapper memberResponseMapper;

    private MemberSocialAccount socialAccount(SocialProvider provider, String email, LocalDateTime linkedAt) {
        return MemberSocialAccount.builder()
                .id(1L)
                .memberId(1L)
                .socialProvider(provider)
                .socialProviderId("providerId")
                .email(email)
                .isEmailSharingEnabled(true)
                .linkedAt(linkedAt)
                .build();
    }

    private LinkedProviderItem itemOf(LinkedProviderResponse response, SocialProvider provider) {
        return response.providers().stream()
                .filter(item -> item.provider() == provider)
                .findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("성공 - 미연동 플랫폼도 포함해 지원 플랫폼 전체를 반환한다")
    void toLinkedProviderResponse_includesEverySupportedProvider() {
        // given - 카카오만 연동한 회원
        SocialAccounts socialAccounts = SocialAccounts.of(List.of(
                socialAccount(SocialProvider.KAKAO, "azit@kakao.com", LocalDateTime.of(2026, 2, 26, 10, 0))));

        // when
        LinkedProviderResponse response = memberResponseMapper.toLinkedProviderResponse(socialAccounts);

        // then
        assertThat(response.providers()).hasSize(SocialProvider.values().length);

        LinkedProviderItem kakao = itemOf(response, SocialProvider.KAKAO);
        assertThat(kakao.isLinked()).isTrue();
        assertThat(kakao.providerName()).isEqualTo("카카오");
        assertThat(kakao.email()).isEqualTo("azit@kakao.com");
        assertThat(kakao.linkedAt()).isEqualTo(LocalDate.of(2026, 2, 26));

        LinkedProviderItem apple = itemOf(response, SocialProvider.APPLE);
        assertThat(apple.isLinked()).isFalse();
        assertThat(apple.providerName()).isEqualTo("애플");
        assertThat(apple.email()).isNull();
        assertThat(apple.linkedAt()).isNull();
    }

    @Test
    @DisplayName("성공 - 연동이 1개뿐이면 해제 불가로 내려간다 (계정 미아 방지)")
    void toLinkedProviderResponse_marksNotUnlinkable_whenOnlyOneProviderLinked() {
        // given
        SocialAccounts socialAccounts = SocialAccounts.of(List.of(
                socialAccount(SocialProvider.KAKAO, "azit@kakao.com", LocalDateTime.now())));

        // when
        LinkedProviderResponse response = memberResponseMapper.toLinkedProviderResponse(socialAccounts);

        // then
        assertThat(itemOf(response, SocialProvider.KAKAO).isUnlinkable()).isFalse();
    }

    @Test
    @DisplayName("성공 - 연동이 2개 이상이면 모두 해제 가능으로 내려간다")
    void toLinkedProviderResponse_marksUnlinkable_whenMultipleProvidersLinked() {
        // given
        SocialAccounts socialAccounts = SocialAccounts.of(List.of(
                socialAccount(SocialProvider.KAKAO, "azit@kakao.com", LocalDateTime.now()),
                socialAccount(SocialProvider.APPLE, null, LocalDateTime.now())));

        // when
        LinkedProviderResponse response = memberResponseMapper.toLinkedProviderResponse(socialAccounts);

        // then
        assertThat(response.providers()).allMatch(LinkedProviderItem::isUnlinkable);
        // 애플이 이메일을 제공하지 않은 경우에도 연동 상태는 그대로 노출된다
        LinkedProviderItem apple = itemOf(response, SocialProvider.APPLE);
        assertThat(apple.isLinked()).isTrue();
        assertThat(apple.email()).isNull();
    }

    @Test
    @DisplayName("성공 - 연동된 소셜이 없어도 전체 플랫폼이 미연동 상태로 반환된다")
    void toLinkedProviderResponse_returnsAllUnlinked_whenNoSocialAccount() {
        // given
        SocialAccounts socialAccounts = SocialAccounts.of(List.of());

        // when
        LinkedProviderResponse response = memberResponseMapper.toLinkedProviderResponse(socialAccounts);

        // then
        assertThat(response.providers()).hasSize(SocialProvider.values().length);
        assertThat(response.providers()).noneMatch(LinkedProviderItem::isLinked);
        assertThat(response.providers()).noneMatch(LinkedProviderItem::isUnlinkable);
    }
}
