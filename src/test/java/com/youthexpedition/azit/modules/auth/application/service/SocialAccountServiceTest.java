package com.youthexpedition.azit.modules.auth.application.service;

import com.youthexpedition.azit.infrastructure.exception.BusinessException;
import com.youthexpedition.azit.modules.auth.application.port.in.command.SocialLoginCommand;
import com.youthexpedition.azit.modules.auth.application.port.in.command.SocialRevokeCommand;
import com.youthexpedition.azit.modules.auth.application.port.out.SocialAuthPort;
import com.youthexpedition.azit.modules.auth.domain.model.SocialProfile;
import com.youthexpedition.azit.modules.auth.domain.model.enums.AuthErrorCode;
import com.youthexpedition.azit.modules.member.application.port.out.LoadMemberSocialAccountPort;
import com.youthexpedition.azit.modules.member.application.port.out.SaveMemberSocialAccountPort;
import com.youthexpedition.azit.modules.member.domain.model.MemberSocialAccount;
import com.youthexpedition.azit.modules.member.fixture.MemberSocialAccountFixture;
import com.youthexpedition.azit.modules.member.domain.model.enums.MemberErrorCode;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SocialAccountService 단위 테스트")
class SocialAccountServiceTest {

    @Mock private SocialAuthPort socialAuthPort;
    @Mock private LoadMemberSocialAccountPort loadMemberSocialAccountPort;
    @Mock private SaveMemberSocialAccountPort saveMemberSocialAccountPort;

    @InjectMocks
    private SocialAccountService socialAccountService;

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;

    private MemberSocialAccount account(Long id, Long memberId, SocialProvider provider, String providerId) {
        return MemberSocialAccountFixture.account(id, memberId, provider, providerId);
    }

    @Nested
    @DisplayName("소셜 계정 연동")
    class Link {

        private final SocialLoginCommand command =
                new SocialLoginCommand(SocialProvider.APPLE, "authCode", null, "idToken", null);

        private final SocialProfile profile = new SocialProfile(
                "appleSub", SocialProvider.APPLE, "appleUser", "test@apple.com", null, "appleRefreshToken", true);

        private void stubProfile() {
            doReturn(profile).when(socialAuthPort).getSocialProfile(command);
        }

        @Test
        @DisplayName("성공 - 미연동 플랫폼이면 소셜 계정이 연동된다")
        void link_success_whenProviderNotLinkedYet() {
            // given - 카카오만 연동된 회원이 애플을 추가 연동
            stubProfile();
            doReturn(List.of(account(1L, MEMBER_ID, SocialProvider.KAKAO, "12345")))
                    .when(loadMemberSocialAccountPort).findAllByMemberId(MEMBER_ID);
            doReturn(Optional.empty()).when(loadMemberSocialAccountPort).findBySocialInfo(SocialProvider.APPLE, "appleSub");

            // when
            socialAccountService.link(MEMBER_ID, command);

            // then
            ArgumentCaptor<MemberSocialAccount> captor = ArgumentCaptor.forClass(MemberSocialAccount.class);
            verify(saveMemberSocialAccountPort).save(captor.capture());

            MemberSocialAccount linked = captor.getValue();
            assertThat(linked.getMemberId()).isEqualTo(MEMBER_ID);
            assertThat(linked.getSocialProvider()).isEqualTo(SocialProvider.APPLE);
            assertThat(linked.getSocialProviderId()).isEqualTo("appleSub");
            assertThat(linked.getAppleRefreshToken()).isEqualTo("appleRefreshToken");
            assertThat(linked.getLinkedAt()).isNotNull();
        }

        @Test
        @DisplayName("실패 - 해당 소셜 계정이 이미 다른 회원에게 연동된 경우 차단")
        void link_throwsException_whenSocialAccountLinkedToAnotherMember() {
            // given
            stubProfile();
            doReturn(List.of(account(1L, MEMBER_ID, SocialProvider.KAKAO, "12345")))
                    .when(loadMemberSocialAccountPort).findAllByMemberId(MEMBER_ID);
            doReturn(Optional.of(account(9L, OTHER_MEMBER_ID, SocialProvider.APPLE, "appleSub")))
                    .when(loadMemberSocialAccountPort).findBySocialInfo(SocialProvider.APPLE, "appleSub");

            // when & then
            assertThatThrownBy(() -> socialAccountService.link(MEMBER_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
            verify(saveMemberSocialAccountPort, never()).save(any(MemberSocialAccount.class));
        }

        @Test
        @DisplayName("실패 - 이미 같은 플랫폼을 연동한 경우 차단")
        void link_throwsException_whenProviderAlreadyLinked() {
            // given
            stubProfile();
            doReturn(List.of(account(1L, MEMBER_ID, SocialProvider.APPLE, "otherAppleSub")))
                    .when(loadMemberSocialAccountPort).findAllByMemberId(MEMBER_ID);

            // when & then
            assertThatThrownBy(() -> socialAccountService.link(MEMBER_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.ALREADY_LINKED_PROVIDER);
            verify(saveMemberSocialAccountPort, never()).save(any(MemberSocialAccount.class));
        }

        @Test
        @DisplayName("실패 - 동시 요청으로 유니크 제약에 걸리면 중복 연동으로 변환")
        void link_throwsException_whenUniqueConstraintViolated() {
            // given - 검증 통과 후 동시에 같은 소셜 계정이 연동된 상황
            stubProfile();
            doReturn(List.of()).when(loadMemberSocialAccountPort).findAllByMemberId(MEMBER_ID);
            doReturn(Optional.empty()).when(loadMemberSocialAccountPort).findBySocialInfo(SocialProvider.APPLE, "appleSub");
            doThrow(new DataIntegrityViolationException("duplicate"))
                    .when(saveMemberSocialAccountPort).save(any(MemberSocialAccount.class));

            // when & then
            assertThatThrownBy(() -> socialAccountService.link(MEMBER_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
        }

        @Test
        @DisplayName("실패 - 자격증명이 없으면 소셜 프로필을 조회하지 않고 차단")
        void link_throwsException_whenCredentialMissing() {
            // given
            SocialLoginCommand emptyCommand =
                    new SocialLoginCommand(SocialProvider.KAKAO, null, null, null, null);

            // when & then
            assertThatThrownBy(() -> socialAccountService.link(MEMBER_ID, emptyCommand))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.MISSING_SOCIAL_CREDENTIAL);
            verify(socialAuthPort, never()).getSocialProfile(any());
        }
    }

    @Nested
    @DisplayName("소셜 계정 연동 해제")
    class Unlink {

        @Test
        @DisplayName("성공 - 플랫폼 연동 해제 후 소셜 계정을 삭제한다")
        void unlink_success_revokesBeforeDeleting() {
            // given
            MemberSocialAccount appleAccount = account(2L, MEMBER_ID, SocialProvider.APPLE, "appleSub");
            doReturn(List.of(account(1L, MEMBER_ID, SocialProvider.KAKAO, "12345"), appleAccount))
                    .when(loadMemberSocialAccountPort).findAllByMemberIdForUpdate(MEMBER_ID);

            // when
            socialAccountService.unlink(MEMBER_ID, SocialProvider.APPLE);

            // then - revoke에 필요한 정보가 사라지기 전에 해제가 끝나야 함
            InOrder order = inOrder(socialAuthPort, saveMemberSocialAccountPort);
            order.verify(socialAuthPort).revoke(SocialRevokeCommand.from(appleAccount));
            order.verify(saveMemberSocialAccountPort).deleteById(2L);
        }

        @Test
        @DisplayName("실패 - 마지막 하나 남은 연동은 해제할 수 없다")
        void unlink_throwsException_whenLastProvider() {
            // given
            doReturn(List.of(account(1L, MEMBER_ID, SocialProvider.KAKAO, "12345")))
                    .when(loadMemberSocialAccountPort).findAllByMemberIdForUpdate(MEMBER_ID);

            // when & then - 플랫폼 연동 해제도 호출되지 않아야 함
            assertThatThrownBy(() -> socialAccountService.unlink(MEMBER_ID, SocialProvider.KAKAO))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.CANNOT_UNLINK_LAST_PROVIDER);
            verify(socialAuthPort, never()).revoke(any());
            verify(saveMemberSocialAccountPort, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("실패 - 연동되지 않은 플랫폼은 해제할 수 없다")
        void unlink_throwsException_whenProviderNotLinked() {
            // given
            doReturn(List.of(account(1L, MEMBER_ID, SocialProvider.KAKAO, "12345"),
                    account(3L, MEMBER_ID, SocialProvider.KAKAO, "67890")))
                    .when(loadMemberSocialAccountPort).findAllByMemberIdForUpdate(MEMBER_ID);

            // when & then
            assertThatThrownBy(() -> socialAccountService.unlink(MEMBER_ID, SocialProvider.APPLE))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.PROVIDER_NOT_LINKED);
            verify(saveMemberSocialAccountPort, never()).deleteById(anyLong());
        }
    }
}
