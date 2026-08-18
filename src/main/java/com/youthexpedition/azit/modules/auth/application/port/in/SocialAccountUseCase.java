package com.youthexpedition.azit.modules.auth.application.port.in;

import com.youthexpedition.azit.modules.auth.application.port.in.command.SocialLoginCommand;
import com.youthexpedition.azit.modules.auth.application.port.in.dto.AppleLinkSessionResponse;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;

public interface SocialAccountUseCase {
    void link(Long memberId, SocialLoginCommand command);
    void unlink(Long memberId, SocialProvider socialProvider);

    AppleLinkSessionResponse createAppleLinkSession(Long memberId, String redirectUrl);
}
