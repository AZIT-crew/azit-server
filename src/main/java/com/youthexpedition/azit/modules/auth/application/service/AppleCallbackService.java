package com.youthexpedition.azit.modules.auth.application.service;

import com.youthexpedition.azit.infrastructure.auth.util.RedirectUrlValidator;
import com.youthexpedition.azit.infrastructure.exception.BusinessException;
import com.youthexpedition.azit.modules.auth.application.port.in.AppleCallbackUseCase;
import com.youthexpedition.azit.modules.auth.application.port.in.SocialAccountUseCase;
import com.youthexpedition.azit.modules.auth.application.port.in.SocialLoginUseCase;
import com.youthexpedition.azit.modules.auth.application.port.in.command.SocialLoginCommand;
import com.youthexpedition.azit.modules.auth.application.port.in.dto.AppleCallbackResult;
import com.youthexpedition.azit.modules.auth.application.port.out.AppleLinkSessionPort;
import com.youthexpedition.azit.modules.auth.domain.model.AppleLinkSession;
import com.youthexpedition.azit.modules.auth.domain.model.AuthResult;
import com.youthexpedition.azit.modules.auth.domain.model.enums.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppleCallbackService implements AppleCallbackUseCase {

    private final SocialLoginUseCase socialLoginUseCase;
    private final SocialAccountUseCase socialAccountUseCase;
    private final AppleLinkSessionPort appleLinkSessionPort;
    private final RedirectUrlValidator redirectUrlValidator;

    private static final String LINK_RESULT_PARAM = "result";
    private static final String LINK_ERROR_PARAM = "error";
    private static final String LINK_RESULT_SUCCESS = "success";
    private static final String LINK_RESULT_FAIL = "fail";

    @Override
    public AppleCallbackResult handle(SocialLoginCommand command, String state) {
        // 세션 조회는 일회용 소비이므로, 없으면 연동이 아닌 일반 로그인 요청
        return appleLinkSessionPort.consume(state)
                .map(session -> handleLink(session, command))
                .orElseGet(() -> handleLogin(command, state));
    }

    private AppleCallbackResult handleLogin(SocialLoginCommand command, String state) {
        // 로그인 흐름은 state에 담긴 프론트 주소로 복귀하므로, 임의의 사이트로 보내지지 않도록 먼저 검증
        redirectUrlValidator.validate(state);

        AuthResult authResult = socialLoginUseCase.login(command);

        return AppleCallbackResult.login(authResult, state);
    }

    private AppleCallbackResult handleLink(AppleLinkSession session, SocialLoginCommand command) {
        String redirectUrl = session.redirectUrl();

        // 세션 발급 이후 허용 목록이 바뀐 경우를 대비해 복귀 직전에 다시 확인
        if (!redirectUrlValidator.isAllowed(redirectUrl)) {
            throw new BusinessException(AuthErrorCode.INVALID_REDIRECT_URL);
        }

        try {
            socialAccountUseCase.link(session.memberId(), command);
            return AppleCallbackResult.link(buildLinkRedirectUrl(redirectUrl, LINK_RESULT_SUCCESS, null));
        } catch (BusinessException e) {
            log.warn("[SOCIAL_ACCOUNT] memberId: {}의 애플 연동에 실패했습니다: {}", session.memberId(), e.getErrorCode().getCode());
            return AppleCallbackResult.link(buildLinkRedirectUrl(redirectUrl, LINK_RESULT_FAIL, e.getErrorCode().getCode()));
        }
    }

    private String buildLinkRedirectUrl(String redirectUrl, String result, String errorCode) {
        return UriComponentsBuilder.fromUriString(redirectUrl)
                .queryParam(LINK_RESULT_PARAM, result)
                .queryParamIfPresent(LINK_ERROR_PARAM, Optional.ofNullable(errorCode))
                .build()
                .toUriString();
    }
}
