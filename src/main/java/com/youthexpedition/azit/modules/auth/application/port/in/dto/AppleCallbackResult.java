package com.youthexpedition.azit.modules.auth.application.port.in.dto;

import com.youthexpedition.azit.modules.auth.domain.model.AuthResult;

/**
 * 애플 콜백 처리 결과.
 * 웹 어댑터는 로그인일 때만 토큰 쿠키를 내려주고, 조립이 끝난 redirectUrl로 리다이렉트하기만 하면 됨
 */
public record AppleCallbackResult(
        AuthResult authResult, // 로그인일 때만 채워짐
        String redirectUrl
) {
    public static AppleCallbackResult login(AuthResult authResult, String redirectUrl) {
        return new AppleCallbackResult(authResult, redirectUrl);
    }

    public static AppleCallbackResult link(String redirectUrl) {
        return new AppleCallbackResult(null, redirectUrl);
    }

    public boolean isLogin() {
        return authResult != null;
    }
}
