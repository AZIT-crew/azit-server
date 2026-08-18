package com.youthexpedition.azit.modules.auth.adapter.in.web;

import com.youthexpedition.azit.infrastructure.auth.util.CookieUtil;
import com.youthexpedition.azit.infrastructure.common.annotation.CurrentAccessToken;
import com.youthexpedition.azit.infrastructure.common.annotation.CurrentMemberId;
import com.youthexpedition.azit.infrastructure.common.response.CommonResponse;
import com.youthexpedition.azit.infrastructure.common.response.code.CommonSuccessCode;
import com.youthexpedition.azit.modules.auth.adapter.in.web.docs.AuthControllerDocs;
import com.youthexpedition.azit.modules.auth.adapter.in.web.dto.AppleNotificationRequest;
import com.youthexpedition.azit.modules.auth.adapter.in.web.dto.CreateAppleLinkSessionRequest;
import com.youthexpedition.azit.modules.auth.adapter.in.web.dto.LinkSocialAccountRequest;
import com.youthexpedition.azit.modules.auth.adapter.in.web.dto.SocialLoginRequest;
import com.youthexpedition.azit.modules.auth.application.port.in.dto.AppleCallbackResult;
import com.youthexpedition.azit.modules.auth.application.port.in.dto.AppleLinkSessionResponse;
import com.youthexpedition.azit.modules.auth.application.port.in.dto.SocialLoginResponse;
import com.youthexpedition.azit.modules.auth.application.port.in.AppleCallbackUseCase;
import com.youthexpedition.azit.modules.auth.application.port.in.AppleNotificationUseCase;
import com.youthexpedition.azit.modules.auth.application.port.in.SocialAccountUseCase;
import com.youthexpedition.azit.modules.auth.application.port.in.SocialLoginUseCase;
import com.youthexpedition.azit.modules.auth.application.port.in.TokenUseCase;
import com.youthexpedition.azit.modules.auth.application.port.in.command.SocialLoginCommand;
import com.youthexpedition.azit.modules.auth.domain.model.AuthResult;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final SocialLoginUseCase socialLoginUseCase;
    private final AppleCallbackUseCase appleCallbackUseCase;
    private final AppleNotificationUseCase appleNotificationUseCase;
    private final SocialAccountUseCase socialAccountUseCase;
    private final TokenUseCase tokenUseCase;
    private final CookieUtil cookieUtil;

    @PostMapping("/social-login/{provider}")
    public CommonResponse<SocialLoginResponse> socialLogin(@PathVariable SocialProvider provider,
                                                           @Valid @RequestBody SocialLoginRequest request, HttpServletResponse response) {
        SocialLoginCommand command = request.toCommand(provider);
        AuthResult authResult = socialLoginUseCase.login(command);
        SocialLoginResponse loginResponse = SocialLoginResponse.from(authResult);

        cookieUtil.setRefreshTokenCookie(response, authResult.authToken().refreshToken());

        return CommonResponse.of(CommonSuccessCode.SUCCESS, loginResponse);
    }

    // 애플 로그인·연동 공통 콜백 (애플 서버가 직접 호출)
    @PostMapping(value = "/social-login/apple", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void appleLogin(@RequestParam("code") String code, @RequestParam("id_token") String idToken,
                           @RequestParam(value = "user", required = false) String user,
                           @RequestParam(value = "state", required = false) String state, HttpServletResponse response) throws IOException {
        SocialLoginCommand command = SocialLoginCommand.of(SocialProvider.APPLE, code, idToken, user);
        AppleCallbackResult callbackResult = appleCallbackUseCase.handle(command, state);

        // 로그인일 경우에만 토큰 세팅
        if (callbackResult.isLogin()) {
            cookieUtil.setRefreshTokenCookie(response, callbackResult.refreshToken());
        }

        // 프론트 페이지로 리다이렉트
        response.sendRedirect(callbackResult.redirectUrl());
    }

    @PostMapping("/reissue")
    public CommonResponse<SocialLoginResponse> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtil.getRefreshToken(request);
        AuthResult authResult = tokenUseCase.reissue(refreshToken);
        SocialLoginResponse loginResponse = SocialLoginResponse.from(authResult);

        cookieUtil.setRefreshTokenCookie(response, authResult.authToken().refreshToken());

        return CommonResponse.of(CommonSuccessCode.SUCCESS, loginResponse);
    }

    @PostMapping("/logout")
    public CommonResponse<Void> logout(@CurrentMemberId Long memberId, @CurrentAccessToken String accessToken, HttpServletResponse response) {
        tokenUseCase.logout(memberId, accessToken);
        cookieUtil.deleteRefreshTokenCookie(response);

        return CommonResponse.of(CommonSuccessCode.SUCCESS);
    }

    @PostMapping("/apple/notification")
    public CommonResponse<Void> receiveAppleNotification(@Valid @RequestBody AppleNotificationRequest request) {
        appleNotificationUseCase.handleNotification(request.payload());

        return CommonResponse.of(CommonSuccessCode.SUCCESS);
    }

    @PostMapping("/social-accounts/apple/link-session")
    public CommonResponse<AppleLinkSessionResponse> createAppleLinkSession(@CurrentMemberId Long memberId,
                                                                           @Valid @RequestBody CreateAppleLinkSessionRequest request) {
        return CommonResponse.of(CommonSuccessCode.SUCCESS,
                socialAccountUseCase.createAppleLinkSession(request.toCommand(memberId)));
    }

    @PostMapping("/social-accounts/{provider}")
    public CommonResponse<Void> linkSocialAccount(@CurrentMemberId Long memberId, @PathVariable SocialProvider provider,
                                                  @Valid @RequestBody LinkSocialAccountRequest request) {
        socialAccountUseCase.link(memberId, request.toCommand(provider));

        return CommonResponse.of(CommonSuccessCode.SUCCESS);
    }

    @DeleteMapping("/social-accounts/{provider}")
    public CommonResponse<Void> unlinkSocialAccount(@CurrentMemberId Long memberId, @PathVariable SocialProvider provider) {
        socialAccountUseCase.unlink(memberId, provider);

        return CommonResponse.of(CommonSuccessCode.SUCCESS);
    }
}
