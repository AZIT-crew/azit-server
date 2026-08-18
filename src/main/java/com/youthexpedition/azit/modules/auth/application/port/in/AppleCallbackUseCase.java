package com.youthexpedition.azit.modules.auth.application.port.in;

import com.youthexpedition.azit.modules.auth.application.port.in.command.SocialLoginCommand;
import com.youthexpedition.azit.modules.auth.application.port.in.dto.AppleCallbackResult;

public interface AppleCallbackUseCase {
    /**
     * 애플이 직접 호출하는 콜백을 처리
     * state가 발급된 연동 세션이면 추가 연동으로, 아니면 로그인으로 처리함
     */
    AppleCallbackResult handle(SocialLoginCommand command, String state);
}
