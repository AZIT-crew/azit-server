package com.youthexpedition.azit.modules.auth.application.service;

import com.youthexpedition.azit.infrastructure.auth.jwt.JwtProvider;
import com.youthexpedition.azit.modules.auth.application.port.in.command.SocialLoginCommand;
import com.youthexpedition.azit.modules.auth.application.port.out.SocialAuthPort;
import com.youthexpedition.azit.modules.auth.application.port.out.TokenPort;
import com.youthexpedition.azit.modules.auth.domain.model.AuthResult;
import com.youthexpedition.azit.modules.auth.domain.model.AuthToken;
import com.youthexpedition.azit.modules.auth.domain.model.SocialProfile;
import com.youthexpedition.azit.modules.crew.application.port.out.LoadCrewMemberPort;
import com.youthexpedition.azit.modules.member.application.port.out.LoadMemberPort;
import com.youthexpedition.azit.modules.member.application.port.out.LoadMemberSocialAccountPort;
import com.youthexpedition.azit.modules.member.application.port.out.LoadTermsVersionPort;
import com.youthexpedition.azit.modules.member.application.port.out.SaveMemberPort;
import com.youthexpedition.azit.modules.member.application.port.out.SaveMemberSocialAccountPort;
import com.youthexpedition.azit.modules.member.domain.model.Member;
import com.youthexpedition.azit.modules.member.domain.model.MemberSocialAccount;
import com.youthexpedition.azit.modules.member.domain.model.TermsVersion;
import com.youthexpedition.azit.modules.member.domain.model.enums.MemberRole;
import com.youthexpedition.azit.modules.member.domain.model.enums.MemberStatus;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import com.youthexpedition.azit.modules.member.domain.model.enums.TermsType;
import com.youthexpedition.azit.modules.member.domain.model.provider.ProfileImageProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SocialLoginService 단위 테스트")
class SocialLoginServiceTest {

    @Mock private SocialAuthPort socialAuthPort;
    @Mock private LoadMemberPort loadMemberPort;
    @Mock private SaveMemberPort saveMemberPort;
    @Mock private LoadMemberSocialAccountPort loadMemberSocialAccountPort;
    @Mock private SaveMemberSocialAccountPort saveMemberSocialAccountPort;
    @Mock private TokenPort tokenPort;
    @Mock private LoadCrewMemberPort loadCrewMemberPort;
    @Mock private LoadTermsVersionPort loadTermsVersionPort;
    @Mock private JwtProvider jwtProvider;
    @Mock private ProfileImageProvider profileImageProvider;

    @InjectMocks
    private SocialLoginService socialLoginService;

    @Nested
    @DisplayName("약관 버전 체크")
    class TermsUpdateCheck {

        private final SocialLoginCommand command = new SocialLoginCommand(
                SocialProvider.KAKAO, "authCode", null, null, null);

        private final Member activeMember = Member.builder()
                .id(1L)
                .nickname("testUser")
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.MEMBER)
                .totalPoints(0L)
                .totalAttendanceCount(0)
                .build();

        private final AuthToken authToken = AuthToken.builder()
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .accessTokenExpiresIn(3600L)
                .build();

        private TermsVersion termsVersion(Long id, TermsType type, boolean isRequired) {
            return TermsVersion.builder()
                    .id(id)
                    .termsType(type)
                    .version("1.0")
                    .isRequired(isRequired)
                    .effectiveAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                    .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                    .build();
        }

        private TermsVersion newTermsVersion(Long id, TermsType type) {
            return TermsVersion.builder()
                    .id(id)
                    .termsType(type)
                    .version("2.0")
                    .isRequired(true)
                    .effectiveAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                    .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                    .build();
        }

        private void stubCommonLogin() {
            SocialProfile profile = new SocialProfile("socialId", SocialProvider.KAKAO, "testUser", null, null, null, false);
            MemberSocialAccount socialAccount = MemberSocialAccount.link(1L, SocialProvider.KAKAO, "socialId", null, false, null);
            doReturn(profile).when(socialAuthPort).getSocialProfile(any());
            doReturn(Optional.of(socialAccount)).when(loadMemberSocialAccountPort).findBySocialInfo(any(), any());
            doReturn(Optional.of(activeMember)).when(loadMemberPort).findById(1L);
            doReturn(activeMember).when(saveMemberPort).save(any());
            doReturn(Optional.empty()).when(loadCrewMemberPort).findRecentJoinedCrewMember(any());
            doReturn("accessToken").when(jwtProvider).generateAccessToken(any(), any(), any());
            doReturn("refreshToken").when(jwtProvider).generateRefreshToken(any());
            doReturn(3600L).when(jwtProvider).getAccessTokenExpirationSeconds();
            doReturn(604800L).when(jwtProvider).getRefreshTokenExpirationSeconds();
        }

        @Test
        @DisplayName("모든 필수 약관에 동의한 경우 needsTermsUpdate = false")
        void login_needsTermsUpdate_false_whenAllRequiredTermsConsented() {
            // given
            stubCommonLogin();
            List<TermsVersion> latestVersions = List.of(
                    termsVersion(1L, TermsType.SERVICE, true),
                    termsVersion(2L, TermsType.PRIVACY, true),
                    termsVersion(3L, TermsType.LOCATION, true),
                    termsVersion(4L, TermsType.THIRD_PARTY, true)
            );
            doReturn(latestVersions).when(loadTermsVersionPort).findAllLatest();
            doReturn(Set.of(1L, 2L, 3L, 4L)).when(loadTermsVersionPort).findConsentedVersionIdsByMemberId(1L);

            // when
            AuthResult result = socialLoginService.login(command);

            // then
            assertFalse(result.needsTermsUpdate());
        }

        @Test
        @DisplayName("새 버전 필수 약관에 미동의한 경우 needsTermsUpdate = true")
        void login_needsTermsUpdate_true_whenNewRequiredTermsVersionExists() {
            // given
            stubCommonLogin();
            List<TermsVersion> latestVersions = List.of(
                    newTermsVersion(10L, TermsType.SERVICE), // v2.0 새 버전
                    termsVersion(2L, TermsType.PRIVACY, true),
                    termsVersion(3L, TermsType.LOCATION, true),
                    termsVersion(4L, TermsType.THIRD_PARTY, true)
            );
            doReturn(latestVersions).when(loadTermsVersionPort).findAllLatest();
            // 멤버는 v1.0(id=1)에만 동의, v2.0(id=10)에는 미동의
            doReturn(Set.of(1L, 2L, 3L, 4L)).when(loadTermsVersionPort).findConsentedVersionIdsByMemberId(1L);

            // when
            AuthResult result = socialLoginService.login(command);

            // then
            assertTrue(result.needsTermsUpdate());
        }

        @Test
        @DisplayName("PENDING_TERMS 상태 멤버는 약관 버전 체크 없이 needsTermsUpdate = false")
        void login_needsTermsUpdate_false_whenMemberIsPendingTerms() {
            // given
            Member pendingMember = Member.builder()
                    .id(2L)
                    .nickname("newUser")
                    .status(MemberStatus.PENDING_TERMS)
                    .role(MemberRole.MEMBER)
                    .totalPoints(0L)
                    .totalAttendanceCount(0)
                    .build();

            SocialProfile profile = new SocialProfile("socialId2", SocialProvider.KAKAO,  "newUser", null, null, null, false);
            MemberSocialAccount socialAccount = MemberSocialAccount.link(2L, SocialProvider.KAKAO, "socialId2", null, false, null);
            doReturn(profile).when(socialAuthPort).getSocialProfile(any());
            doReturn(Optional.of(socialAccount)).when(loadMemberSocialAccountPort).findBySocialInfo(any(), any());
            doReturn(Optional.of(pendingMember)).when(loadMemberPort).findById(2L);
            doReturn(pendingMember).when(saveMemberPort).save(any());
            doReturn("accessToken").when(jwtProvider).generateAccessToken(any(), any(), any());
            doReturn("refreshToken").when(jwtProvider).generateRefreshToken(any());
            doReturn(3600L).when(jwtProvider).getAccessTokenExpirationSeconds();
            doReturn(604800L).when(jwtProvider).getRefreshTokenExpirationSeconds();

            // when
            AuthResult result = socialLoginService.login(command);

            // then
            assertFalse(result.needsTermsUpdate());
        }
    }

    @Nested
    @DisplayName("소셜 계정 매핑 기반 로그인")
    class SocialAccountLookup {

        private void stubTokenIssue() {
            doReturn("accessToken").when(jwtProvider).generateAccessToken(any(), any(), any());
            doReturn("refreshToken").when(jwtProvider).generateRefreshToken(any());
            doReturn(3600L).when(jwtProvider).getAccessTokenExpirationSeconds();
            doReturn(604800L).when(jwtProvider).getRefreshTokenExpirationSeconds();
        }

        private Member pendingMember(Long id) {
            return Member.builder()
                    .id(id)
                    .nickname("newUser")
                    .status(MemberStatus.PENDING_TERMS)
                    .role(MemberRole.MEMBER)
                    .totalPoints(0L)
                    .totalAttendanceCount(0)
                    .build();
        }

        @Test
        @DisplayName("연동된 소셜 계정이 없으면 신규 가입 후 소셜 계정을 연동한다")
        void login_registersMemberAndLinksSocialAccount_whenSocialAccountNotFound() {
            // given
            SocialProfile profile = new SocialProfile(
                    "newSocialId", SocialProvider.KAKAO, "newUser", "new@kakao.com", null, null, true);
            doReturn(profile).when(socialAuthPort).getSocialProfile(any());
            doReturn(Optional.empty()).when(loadMemberSocialAccountPort).findBySocialInfo(SocialProvider.KAKAO, "newSocialId");
            doReturn("/default/member/default_1.svg").when(profileImageProvider).getRandomDefaultImage();
            doReturn(pendingMember(10L)).when(saveMemberPort).save(any());
            stubTokenIssue();

            // when
            socialLoginService.login(new SocialLoginCommand(SocialProvider.KAKAO, "authCode", null, null, null));

            // then - 저장된 회원 ID로 소셜 계정이 연동되어야 함
            ArgumentCaptor<MemberSocialAccount> captor = ArgumentCaptor.forClass(MemberSocialAccount.class);
            verify(saveMemberSocialAccountPort).save(captor.capture());

            MemberSocialAccount linked = captor.getValue();
            assertEquals(10L, linked.getMemberId());
            assertEquals(SocialProvider.KAKAO, linked.getSocialProvider());
            assertEquals("newSocialId", linked.getSocialProviderId());
            assertEquals("new@kakao.com", linked.getEmail());
            assertTrue(linked.isEmailSharingEnabled());
        }

        @Test
        @DisplayName("연동된 소셜 계정이 있으면 신규 가입 없이 매핑된 회원으로 로그인한다")
        void login_reusesMappedMember_whenSocialAccountFound() {
            // given
            SocialProfile profile = new SocialProfile(
                    "socialId", SocialProvider.KAKAO, "newUser", null, null, null, false);
            MemberSocialAccount socialAccount =
                    MemberSocialAccount.link(20L, SocialProvider.KAKAO, "socialId", null, false, null);
            doReturn(profile).when(socialAuthPort).getSocialProfile(any());
            doReturn(Optional.of(socialAccount)).when(loadMemberSocialAccountPort).findBySocialInfo(any(), any());
            doReturn(Optional.of(pendingMember(20L))).when(loadMemberPort).findById(20L);
            doReturn(pendingMember(20L)).when(saveMemberPort).save(any());
            stubTokenIssue();

            // when
            socialLoginService.login(new SocialLoginCommand(SocialProvider.KAKAO, "authCode", null, null, null));

            // then - 새 연동을 만들지 않고 기존 매핑을 그대로 사용해야 함
            verify(saveMemberSocialAccountPort, never()).save(any());
            verify(loadMemberPort).findById(20L);
        }

        @Test
        @DisplayName("애플 재로그인 시 리프레시 토큰이 소셜 계정에 갱신된다")
        void login_updatesAppleRefreshTokenOnSocialAccount_whenProfileHasRefreshToken() {
            // given
            SocialProfile profile = new SocialProfile(
                    "appleSub", SocialProvider.APPLE, "appleUser", null, null, "newAppleRefreshToken", false);
            MemberSocialAccount socialAccount =
                    MemberSocialAccount.link(30L, SocialProvider.APPLE, "appleSub", null, false, "oldAppleRefreshToken");
            doReturn(profile).when(socialAuthPort).getSocialProfile(any());
            doReturn(Optional.of(socialAccount)).when(loadMemberSocialAccountPort).findBySocialInfo(any(), any());
            doReturn(Optional.of(pendingMember(30L))).when(loadMemberPort).findById(30L);
            doReturn(pendingMember(30L)).when(saveMemberPort).save(any());
            stubTokenIssue();

            // when
            socialLoginService.login(SocialLoginCommand.of(SocialProvider.APPLE, "authCode", "idToken", null));

            // then - 토큰은 Member가 아니라 해당 소셜 계정에 저장되어야 함
            ArgumentCaptor<MemberSocialAccount> captor = ArgumentCaptor.forClass(MemberSocialAccount.class);
            verify(saveMemberSocialAccountPort).save(captor.capture());
            assertEquals("newAppleRefreshToken", captor.getValue().getAppleRefreshToken());
        }
    }
}
