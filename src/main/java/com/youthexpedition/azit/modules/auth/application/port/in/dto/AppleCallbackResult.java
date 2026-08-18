package com.youthexpedition.azit.modules.auth.application.port.in.dto;

import com.youthexpedition.azit.modules.auth.domain.model.AuthResult;

/**
 * 애플 콜백 처리 결과.
 * 웹 어댑터는 로그인일 때만 리프레시 토큰 쿠키를 내려주고, 조립이 끝난 redirectUrl로 리다이렉트하기만 하면 된다.
 */
public record AppleCallbackResult(
        String refreshToken, // 로그인일 때만 채워진다
        String redirectUrl   // 검증·조립이 끝난 최종 복귀 주소
) {
    public static AppleCallbackResult login(AuthResult authResult, String redirectUrl) {
        return new AppleCallbackResult(authResult.authToken().refreshToken(), redirectUrl);
    }

    public static AppleCallbackResult link(String redirectUrl) {
        return new AppleCallbackResult(null, redirectUrl);
    }

    public boolean isLogin() {
        return refreshToken != null;
    }
}
