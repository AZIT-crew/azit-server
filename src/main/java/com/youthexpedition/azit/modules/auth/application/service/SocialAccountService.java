package com.youthexpedition.azit.modules.auth.application.service;

import com.youthexpedition.azit.infrastructure.exception.BusinessException;
import com.youthexpedition.azit.modules.auth.application.port.in.SocialAccountUseCase;
import com.youthexpedition.azit.modules.auth.application.port.in.command.SocialLoginCommand;
import com.youthexpedition.azit.modules.auth.application.port.in.command.SocialRevokeCommand;
import com.youthexpedition.azit.modules.auth.application.port.out.SocialAuthPort;
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

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SocialAccountService implements SocialAccountUseCase {
    private final SocialAuthPort socialAuthPort;
    private final LoadMemberSocialAccountPort loadMemberSocialAccountPort;
    private final SaveMemberSocialAccountPort saveMemberSocialAccountPort;

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
     * 소셜 계정 연동을 해제한다. 프로필·활동 데이터는 삭제하지 않고 계정에 그대로 유지된다.
     * 마지막 하나 남은 연동은 해제할 수 없다(계정 미아 방지).
     */
    @Override
    public void unlink(Long memberId, SocialProvider socialProvider) {
        // 동시 요청이 각각 다른 플랫폼을 해제해 연동이 0개가 되지 않도록 잠금 후 판정한다
        SocialAccounts socialAccounts = SocialAccounts.of(loadMemberSocialAccountPort.findAllByMemberIdForUpdate(memberId));
        MemberSocialAccount target = socialAccounts.unlink(socialProvider);

        // 플랫폼 연동 해제는 revoke 정보가 사라지기 전에 수행
        socialAuthPort.revoke(SocialRevokeCommand.from(target));
        saveMemberSocialAccountPort.deleteById(target.getId());

        log.info("[SOCIAL_ACCOUNT] memberId: {}의 {} 연동이 해제되었습니다.", memberId, socialProvider);
    }
}
