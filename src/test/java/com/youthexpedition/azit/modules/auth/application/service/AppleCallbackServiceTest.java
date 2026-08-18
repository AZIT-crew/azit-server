package com.youthexpedition.azit.modules.auth.application.service;

import com.youthexpedition.azit.infrastructure.auth.util.RedirectUrlValidator;
import com.youthexpedition.azit.infrastructure.exception.BusinessException;
import com.youthexpedition.azit.modules.auth.application.port.in.SocialAccountUseCase;
import com.youthexpedition.azit.modules.auth.application.port.in.SocialLoginUseCase;
import com.youthexpedition.azit.modules.auth.application.port.in.command.SocialLoginCommand;
import com.youthexpedition.azit.modules.auth.application.port.in.dto.AppleCallbackResult;
import com.youthexpedition.azit.modules.auth.application.port.out.AppleLinkSessionPort;
import com.youthexpedition.azit.modules.auth.domain.model.AppleLinkSession;
import com.youthexpedition.azit.modules.auth.domain.model.AuthResult;
import com.youthexpedition.azit.modules.auth.domain.model.AuthToken;
import com.youthexpedition.azit.modules.auth.domain.model.enums.AuthErrorCode;
import com.youthexpedition.azit.modules.member.domain.model.enums.MemberStatus;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppleCallbackService 단위 테스트")
class AppleCallbackServiceTest {

    @Mock private SocialLoginUseCase socialLoginUseCase;
    @Mock private SocialAccountUseCase socialAccountUseCase;
    @Mock private AppleLinkSessionPort appleLinkSessionPort;
    @Mock private RedirectUrlValidator redirectUrlValidator;

    @InjectMocks
    private AppleCallbackService appleCallbackService;

    private static final Long MEMBER_ID = 1L;
    private static final String LINK_REDIRECT_URL = "https://azitcrew.com/settings/accounts";
    private static final String LOGIN_REDIRECT_URL = "https://azitcrew.com/login/callback";

    private final SocialLoginCommand command =
            SocialLoginCommand.of(SocialProvider.APPLE, "authCode", "idToken", null);

    private AuthResult authResult() {
        return AuthResult.builder()
                .authToken(AuthToken.builder().accessToken("at").refreshToken("rt").accessTokenExpiresIn(1800L).build())
                .status(MemberStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("로그인 요청")
    class Login {

        @Test
        @DisplayName("성공 - 연동 세션이 없는 state는 로그인으로 처리하고 state 주소로 복귀한다")
        void handle_success_whenNoLinkSession() {
            // given
            doReturn(Optional.empty()).when(appleLinkSessionPort).consume(LOGIN_REDIRECT_URL);
            AuthResult expected = authResult();
            doReturn(expected).when(socialLoginUseCase).login(command);

            // when
            AppleCallbackResult result = appleCallbackService.handle(command, LOGIN_REDIRECT_URL);

            // then
            assertThat(result.isLogin()).isTrue();
            assertThat(result.authResult()).isEqualTo(expected);
            assertThat(result.redirectUrl()).isEqualTo(LOGIN_REDIRECT_URL);
            verify(socialAccountUseCase, never()).link(anyLong(), any());
        }

        @Test
        @DisplayName("실패 - 허용되지 않은 복귀 주소면 로그인하지 않고 차단한다")
        void handle_throwsException_whenRedirectUrlNotAllowed() {
            // given - 애플이 돌려준 state를 그대로 리다이렉트하면 오픈 리다이렉트가 된다
            doReturn(Optional.empty()).when(appleLinkSessionPort).consume("https://evil.com");
            doThrow(new BusinessException(AuthErrorCode.INVALID_REDIRECT_URL))
                    .when(redirectUrlValidator).validate("https://evil.com");

            // when & then
            assertThatThrownBy(() -> appleCallbackService.handle(command, "https://evil.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_REDIRECT_URL);
            verify(socialLoginUseCase, never()).login(any());
        }
    }

    @Nested
    @DisplayName("연동 요청")
    class Link {

        private void stubLinkSession() {
            doReturn(Optional.of(new AppleLinkSession(MEMBER_ID, LINK_REDIRECT_URL)))
                    .when(appleLinkSessionPort).consume("state");
            doReturn(true).when(redirectUrlValidator).isAllowed(LINK_REDIRECT_URL);
        }

        @Test
        @DisplayName("성공 - 세션의 회원에 연동하고 성공 결과를 쿼리로 전달한다")
        void handle_success_whenLinkSessionExists() {
            // given
            stubLinkSession();

            // when
            AppleCallbackResult result = appleCallbackService.handle(command, "state");

            // then - 이미 로그인된 회원의 요청이므로 토큰은 새로 발급하지 않는다
            verify(socialAccountUseCase).link(MEMBER_ID, command);
            verify(socialLoginUseCase, never()).login(any());
            assertThat(result.isLogin()).isFalse();
            assertThat(result.redirectUrl()).isEqualTo(LINK_REDIRECT_URL + "?result=success");
        }

        @Test
        @DisplayName("성공 - 연동에 실패해도 예외 대신 에러 코드를 쿼리로 전달한다")
        void handle_returnsFailResult_whenLinkFails() {
            // given - 웹뷰로 돌아가는 흐름이라 JSON 에러 본문을 노출하면 안 된다
            stubLinkSession();
            doThrow(new BusinessException(AuthErrorCode.ALREADY_LINKED_PROVIDER))
                    .when(socialAccountUseCase).link(MEMBER_ID, command);

            // when
            AppleCallbackResult result = appleCallbackService.handle(command, "state");

            // then
            assertThat(result.isLogin()).isFalse();
            assertThat(result.redirectUrl())
                    .isEqualTo(LINK_REDIRECT_URL + "?result=fail&error=ALREADY_LINKED_PROVIDER");
        }

        @Test
        @DisplayName("성공 - 복귀 주소에 이미 쿼리가 있으면 결과 파라미터를 덧붙인다")
        void handle_appendsResultToExistingQuery() {
            // given
            String redirectUrlWithQuery = LINK_REDIRECT_URL + "?tab=social";
            doReturn(Optional.of(new AppleLinkSession(MEMBER_ID, redirectUrlWithQuery)))
                    .when(appleLinkSessionPort).consume("state");
            doReturn(true).when(redirectUrlValidator).isAllowed(redirectUrlWithQuery);

            // when
            AppleCallbackResult result = appleCallbackService.handle(command, "state");

            // then
            assertThat(result.redirectUrl()).isEqualTo(redirectUrlWithQuery + "&result=success");
        }

        @Test
        @DisplayName("실패 - 세션 발급 이후 허용 목록에서 빠진 주소면 연동하지 않는다")
        void handle_throwsException_whenSessionRedirectUrlNoLongerAllowed() {
            // given
            doReturn(Optional.of(new AppleLinkSession(MEMBER_ID, LINK_REDIRECT_URL)))
                    .when(appleLinkSessionPort).consume("state");
            doReturn(false).when(redirectUrlValidator).isAllowed(LINK_REDIRECT_URL);

            // when & then
            assertThatThrownBy(() -> appleCallbackService.handle(command, "state"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_REDIRECT_URL);
            verify(socialAccountUseCase, never()).link(anyLong(), any());
        }
    }
}
