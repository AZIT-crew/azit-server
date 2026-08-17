package com.youthexpedition.azit.modules.auth.adapter.out.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youthexpedition.azit.infrastructure.auth.util.AppleJwtUtils;
import com.youthexpedition.azit.infrastructure.exception.BusinessException;
import com.youthexpedition.azit.modules.auth.adapter.out.external.dto.ApplePublicKeyResponse;
import com.youthexpedition.azit.modules.auth.adapter.out.external.feign.AppleFeignClient;
import com.youthexpedition.azit.modules.auth.application.port.in.command.SocialLoginCommand;
import com.youthexpedition.azit.modules.auth.domain.model.enums.AuthErrorCode;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppleAuthAdapter 단위 테스트")
class AppleAuthAdapterTest {

    @Mock private AppleFeignClient appleFeignClient;
    @Mock private AppleJwtUtils appleJwtUtils;
    @Mock private Claims claims;

    @InjectMocks
    private AppleAuthAdapter appleAuthAdapter;

    private final SocialLoginCommand command =
            SocialLoginCommand.of(SocialProvider.APPLE, "authCode", "idToken", null);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(appleAuthAdapter, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(appleAuthAdapter, "clientId", "clientId");
        ReflectionTestUtils.setField(appleAuthAdapter, "redirectUrl", "redirectUrl");
    }

    // ID 토큰 검증까지는 통과하고 토큰 요청 단계에서 실패하는 상황을 만든다
    private void stubVerifiedIdToken() {
        ApplePublicKeyResponse.ApplePublicKey matchedKey =
                new ApplePublicKeyResponse.ApplePublicKey("RSA", "kid", "sig", "RS256", "n", "e");
        doReturn(new ApplePublicKeyResponse(List.of(matchedKey))).when(appleFeignClient).getApplePublicKeys();
        doReturn("kid").when(appleJwtUtils).getKidFromHeader(anyString());
        doReturn("clientSecret").when(appleJwtUtils).createClientSecret();
    }

    private FeignException.BadRequest badRequest(String responseBody) {
        Request request = Request.create(Request.HttpMethod.POST, "https://appleid.apple.com/auth/token",
                Map.of(), new byte[0], StandardCharsets.UTF_8, new RequestTemplate());

        return new FeignException.BadRequest(
                "Bad Request", request, responseBody.getBytes(StandardCharsets.UTF_8), Map.of());
    }

    @Test
    @DisplayName("실패 - invalid_grant는 인가 코드 오류로 변환한다")
    void getSocialProfile_throwsInvalidSocialCode_whenInvalidGrant() {
        // given - 만료되었거나 이미 사용된 인가 코드
        stubVerifiedIdToken();
        doReturn(claims).when(appleJwtUtils).verifyIdToken(anyString(), any());
        doThrowBadRequest("{\"error\":\"invalid_grant\"}");

        // when & then
        assertThatThrownBy(() -> appleAuthAdapter.getSocialProfile(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_SOCIAL_CODE);
    }

    @Test
    @DisplayName("실패 - invalid_client는 서버 원인이므로 소셜 인증 실패로 변환한다")
    void getSocialProfile_throwsAuthenticationFailed_whenInvalidClient() {
        // given - 클라이언트 시크릿이 만료되었거나 서명이 잘못된 경우
        stubVerifiedIdToken();
        doReturn(claims).when(appleJwtUtils).verifyIdToken(anyString(), any());
        doThrowBadRequest("{\"error\":\"invalid_client\"}");

        // when & then
        assertThatThrownBy(() -> appleAuthAdapter.getSocialProfile(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.SOCIAL_AUTHENTICATION_FAILED);
    }

    @Test
    @DisplayName("실패 - 응답 본문을 파싱할 수 없으면 소셜 인증 실패로 변환한다")
    void getSocialProfile_throwsAuthenticationFailed_whenResponseNotParsable() {
        // given
        stubVerifiedIdToken();
        doReturn(claims).when(appleJwtUtils).verifyIdToken(anyString(), any());
        doThrowBadRequest("not a json");

        // when & then
        assertThatThrownBy(() -> appleAuthAdapter.getSocialProfile(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.SOCIAL_AUTHENTICATION_FAILED);
    }

    private void doThrowBadRequest(String responseBody) {
        org.mockito.Mockito.doThrow(badRequest(responseBody))
                .when(appleFeignClient).getToken(anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
