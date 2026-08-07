package com.youthexpedition.azit.modules.auth.application.service;

import com.youthexpedition.azit.infrastructure.auth.jwt.JwtProvider;
import com.youthexpedition.azit.infrastructure.exception.BusinessException;
import com.youthexpedition.azit.modules.auth.application.port.in.SocialLoginUseCase;
import com.youthexpedition.azit.modules.auth.application.port.in.command.SocialLoginCommand;
import com.youthexpedition.azit.modules.auth.application.port.out.TokenPort;
import com.youthexpedition.azit.modules.auth.application.port.out.SocialAuthPort;
import com.youthexpedition.azit.modules.auth.domain.model.AuthResult;
import com.youthexpedition.azit.modules.auth.domain.model.enums.AuthErrorCode;
import com.youthexpedition.azit.modules.auth.domain.model.AuthToken;
import com.youthexpedition.azit.modules.auth.domain.model.SocialProfile;
import com.youthexpedition.azit.modules.crew.application.port.out.LoadCrewMemberPort;
import com.youthexpedition.azit.modules.crew.domain.model.CrewMember;
import com.youthexpedition.azit.modules.member.application.port.out.LoadMemberPort;
import com.youthexpedition.azit.modules.member.application.port.out.LoadMemberSocialAccountPort;
import com.youthexpedition.azit.modules.member.application.port.out.LoadTermsVersionPort;
import com.youthexpedition.azit.modules.member.application.port.out.SaveMemberPort;
import com.youthexpedition.azit.modules.member.application.port.out.SaveMemberSocialAccountPort;
import com.youthexpedition.azit.modules.member.domain.model.Member;
import com.youthexpedition.azit.modules.member.domain.model.MemberSocialAccount;
import com.youthexpedition.azit.modules.member.domain.model.TermsVersion;
import com.youthexpedition.azit.modules.member.domain.model.enums.MemberErrorCode;
import com.youthexpedition.azit.modules.member.domain.model.enums.MemberStatus;
import com.youthexpedition.azit.modules.member.domain.model.provider.ProfileImageProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SocialLoginService implements SocialLoginUseCase {
    private final SocialAuthPort socialAuthPort;
    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;
    private final LoadMemberSocialAccountPort loadMemberSocialAccountPort;
    private final SaveMemberSocialAccountPort saveMemberSocialAccountPort;
    private final TokenPort tokenPort;
    private final LoadCrewMemberPort loadCrewMemberPort;
    private final LoadTermsVersionPort loadTermsVersionPort;
    private final JwtProvider jwtProvider;
    private final ProfileImageProvider profileImageProvider;

    @Override
    public AuthResult login(SocialLoginCommand command) {
        command.validateCredential();

        SocialProfile profile = socialAuthPort.getSocialProfile(command);

        // 기존 회원 확인 및 신규 회원 가입
        Member savedMember = upsertMember(profile);

        Long crewId = null;
        boolean needsTermsUpdate = false;
        // ACTIVE 상태일 경우 가장 최근에 가입한 크루 조회 및 약관 버전 체크
        if (savedMember.getStatus() == MemberStatus.ACTIVE) {
            crewId = loadCrewMemberPort.findRecentJoinedCrewMember(savedMember.getId())
                    .map(CrewMember::getCrewId)
                    .orElse(null);
            needsTermsUpdate = hasRequiredTermsUpdate(savedMember.getId());
        }

        // 토큰 생성
        AuthToken authToken = AuthToken.builder()
                .accessToken(jwtProvider.generateAccessToken(savedMember.getId(), savedMember.getRole(), savedMember.getStatus()))
                .refreshToken(jwtProvider.generateRefreshToken(savedMember.getId()))
                .accessTokenExpiresIn(jwtProvider.getAccessTokenExpirationSeconds())
                .build();

        // Redis에 Refresh Token 저장
        saveRefreshToken(savedMember.getId(), authToken.refreshToken());

        return AuthResult.builder()
                .authToken(authToken)
                .status(savedMember.getStatus())
                .crewId(crewId)
                .needsTermsUpdate(needsTermsUpdate)
                .build();
    }

    /**
     * 소셜 계정 매핑을 기준으로 기존 회원을 찾고, 없으면 신규 가입
     * 이메일이 같더라도 자동으로 기존 계정에 병합하지 않음
     */
    private Member upsertMember(SocialProfile profile) {
        return loadMemberSocialAccountPort.findBySocialInfo(profile.socialProvider(), profile.socialProviderId())
                .map(socialAccount -> loginExistingMember(socialAccount, profile))
                .orElseGet(() -> registerMember(profile));
    }

    // 기존 계정으로 로그인
    private Member loginExistingMember(MemberSocialAccount socialAccount, SocialProfile profile) {
        Member member = loadMemberPort.findById(socialAccount.getMemberId())
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 탈퇴한 회원인 경우 재활성화 (유예기간 만료 또는 파기 완료 시 예외 발생)
        if (member.isWithdrawn()) {
            member.reactivate(LocalDateTime.now());
            log.info("[SOCIAL_LOGIN] 탈퇴했던 회원 memberId: {}, socialProviderId: {} 가 재로그인하여 계정이 재활성화 되었습니다.", member.getId(), profile.socialProviderId());
        }

        // 애플 리프레시 토큰이 존재하는 경우 최신값으로 업데이트
        if (profile.refreshToken() != null) {
            socialAccount.updateAppleRefreshToken(profile.refreshToken());
            saveMemberSocialAccountPort.save(socialAccount);
        }

        return saveMemberPort.save(member);
    }

    // 신규 계정 등록
    private Member registerMember(SocialProfile profile) {
        // 제공받은 프로필 이미지가 없을 경우 랜덤으로 기본 이미지 설정
        String profileImageUrl = profile.profileImageUrl();
        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            profileImageUrl = profileImageProvider.getRandomDefaultImage();
        }

        Member savedMember = saveMemberPort.save(Member.create(profile.nickname(), profile.email(), profileImageUrl));

        saveMemberSocialAccountPort.save(MemberSocialAccount.link(
                savedMember.getId(),
                profile.socialProvider(),
                profile.socialProviderId(),
                profile.email(),
                profile.isEmailSharingEnabled(),
                profile.refreshToken()
        ));

        return savedMember;
    }

    private boolean hasRequiredTermsUpdate(Long memberId) {
        List<TermsVersion> latestVersions = loadTermsVersionPort.findAllLatest();
        Set<Long> consentedVersionIds = loadTermsVersionPort.findConsentedVersionIdsByMemberId(memberId);
        return latestVersions.stream()
                .filter(TermsVersion::isRequired)
                .anyMatch(v -> !consentedVersionIds.contains(v.getId()));
    }

    private void saveRefreshToken(Long memberId, String refreshToken) {
        tokenPort.save(memberId, refreshToken, jwtProvider.getRefreshTokenExpirationSeconds());
    }
}
