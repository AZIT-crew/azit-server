package com.youthexpedition.azit.modules.auth.application.service;

import com.youthexpedition.azit.infrastructure.auth.util.RedirectUrlValidator;
import com.youthexpedition.azit.infrastructure.exception.BusinessException;
import com.youthexpedition.azit.modules.auth.application.port.in.SocialAccountUseCase;
import com.youthexpedition.azit.modules.auth.application.port.in.command.CreateAppleLinkSessionCommand;
import com.youthexpedition.azit.modules.auth.application.port.in.command.SocialLoginCommand;
import com.youthexpedition.azit.modules.auth.application.port.in.command.SocialRevokeCommand;
import com.youthexpedition.azit.modules.auth.application.port.in.dto.AppleLinkSessionResponse;
import com.youthexpedition.azit.modules.auth.application.port.out.AppleLinkSessionPort;
import com.youthexpedition.azit.modules.auth.application.port.out.SocialAuthPort;
import com.youthexpedition.azit.modules.auth.domain.model.AppleLinkSession;
import com.youthexpedition.azit.modules.auth.domain.model.SocialProfile;
import com.youthexpedition.azit.modules.auth.domain.model.enums.AuthErrorCode;
import com.youthexpedition.azit.modules.member.application.port.out.LoadMemberSocialAccountPort;
import com.youthexpedition.azit.modules.member.application.port.out.SaveMemberSocialAccountPort;
import com.youthexpedition.azit.modules.member.domain.model.MemberSocialAccount;
import com.youthexpedition.azit.modules.member.domain.model.SocialAccounts;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SocialAccountService implements SocialAccountUseCase {
    private final SocialAuthPort socialAuthPort;
    private final LoadMemberSocialAccountPort loadMemberSocialAccountPort;
    private final SaveMemberSocialAccountPort saveMemberSocialAccountPort;
    private final AppleLinkSessionPort appleLinkSessionPort;
    private final RedirectUrlValidator redirectUrlValidator;

    // 애플 인증 화면에서 사용자가 인증을 마치기까지의 여유 시간
    private static final long APPLE_LINK_SESSION_TTL_SECONDS = 300;

    /**
     * 로그인 중인 회원에 소셜 계정을 추가로 연동한다.
     * 로그인과 동일하게 소셜 자격증명을 검증해 프로필을 받아오므로, 계정 소유자만 연동할 수 있다.
     */
    @Override
    public void link(Long memberId, SocialLoginCommand command) {
        command.validateCredential();

        SocialProfile profile = socialAuthPort.getSocialProfile(command);
        SocialAccounts socialAccounts = SocialAccounts.of(loadMemberSocialAccountPort.findAllByMemberId(memberId));

        // 같은 플랫폼을 이미 연동한 경우 (한 플랫폼당 계정 1개)
        if (socialAccounts.hasProvider(profile.socialProvider())) {
            throw new BusinessException(AuthErrorCode.ALREADY_LINKED_PROVIDER);
        }

        // 타 회원에게 이미 매핑된 소셜 계정인 경우
        if (loadMemberSocialAccountPort.findBySocialInfo(profile.socialProvider(), profile.socialProviderId()).isPresent()) {
            throw new BusinessException(AuthErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
        }

        try {
            saveMemberSocialAccountPort.save(MemberSocialAccount.link(
                    memberId,
                    profile.socialProvider(),
                    profile.socialProviderId(),
                    profile.email(),
                    profile.isEmailSharingEnabled(),
                    profile.refreshToken()
            ));
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 소셜 계정을 연동한 경우 유니크 제약 체크
            log.warn("[SOCIAL_ACCOUNT] memberId: {}의 {} 연동이 유니크 제약에 걸렸습니다.", memberId, profile.socialProvider());
            throw new BusinessException(AuthErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
        }

        log.info("[SOCIAL_ACCOUNT] memberId: {}에 {} 계정이 연동되었습니다.", memberId, profile.socialProvider());
    }

    /**
     * 애플 연동을 시작할 회원을 식별하기 위한 일회용 state를 발급한다.
     * 클라이언트는 이 값을 애플 인증 요청의 state 파라미터에 그대로 실어 보내고,
     * 애플이 콜백에 되돌려준 state로 서버가 연동 대상 회원을 복원한다.
     */
    @Override
    public AppleLinkSessionResponse createAppleLinkSession(CreateAppleLinkSessionCommand command) {
        Long memberId = command.memberId();

        // 연동 후 임의의 사이트로 보내지지 않도록 허용된 복귀 주소만 세션에 담음
        redirectUrlValidator.validate(command.redirectUrl());

        // 이미 애플을 연동한 회원이라면 애플 인증 화면까지 보내기 전에 미리 차단
        SocialAccounts socialAccounts = SocialAccounts.of(loadMemberSocialAccountPort.findAllByMemberId(memberId));
        if (socialAccounts.hasProvider(SocialProvider.APPLE)) {
            throw new BusinessException(AuthErrorCode.ALREADY_LINKED_PROVIDER);
        }

        String state = UUID.randomUUID().toString().replace("-", "");
        appleLinkSessionPort.save(state, new AppleLinkSession(memberId, command.redirectUrl()), APPLE_LINK_SESSION_TTL_SECONDS);

        log.info("[SOCIAL_ACCOUNT] memberId: {}의 애플 연동 세션이 발급되었습니다.", memberId);

        return AppleLinkSessionResponse.of(state);
    }

    /**
     * 소셜 계정 연동을 해제한다. 프로필·활동 데이터는 삭제하지 않고 계정에 그대로 유지된다.
     * 마지막 하나 남은 연동은 해제할 수 없다(계정 미아 방지).
     */
    @Override
    public void unlink(Long memberId, SocialProvider socialProvider) {
        // 동시 요청이 각각 다른 플랫폼을 해제해 연동이 0개가 되지 않도록 잠금 후 판정
        SocialAccounts socialAccounts = SocialAccounts.of(loadMemberSocialAccountPort.findAllByMemberIdForUpdate(memberId));
        MemberSocialAccount target = socialAccounts.unlink(socialProvider);

        // 플랫폼 연동 해제는 revoke 정보가 사라지기 전에 수행
        socialAuthPort.revoke(SocialRevokeCommand.from(target));
        saveMemberSocialAccountPort.deleteById(target.getId());

        log.info("[SOCIAL_ACCOUNT] memberId: {}의 {} 연동이 해제되었습니다.", memberId, socialProvider);
    }
}
