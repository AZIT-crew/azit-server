package com.youthexpedition.azit.modules.member.application.service;

import com.youthexpedition.azit.infrastructure.exception.BusinessException;
import com.youthexpedition.azit.modules.auth.application.port.out.TokenPort;
import com.youthexpedition.azit.modules.crew.application.port.out.LoadCrewMemberPort;
import com.youthexpedition.azit.modules.crew.application.port.out.LoadCrewPort;
import com.youthexpedition.azit.modules.crew.application.port.out.SaveCrewMemberPort;
import com.youthexpedition.azit.modules.crew.application.port.out.SaveCrewPort;
import com.youthexpedition.azit.modules.crew.domain.model.Crew;
import com.youthexpedition.azit.modules.crew.domain.model.CrewMember;
import com.youthexpedition.azit.modules.crew.domain.model.enums.CrewErrorCode;
import com.youthexpedition.azit.modules.crew.domain.model.enums.CrewMemberRole;
import com.youthexpedition.azit.modules.crew.domain.model.enums.CrewMemberStatus;
import com.youthexpedition.azit.modules.member.application.port.in.MemberUseCase;
import com.youthexpedition.azit.infrastructure.common.util.image.ImageUpdateUtil;
import com.youthexpedition.azit.modules.member.application.port.in.command.AgreeToTermsCommand;
import com.youthexpedition.azit.modules.member.application.port.in.command.UpdateMemberProfileCommand;
import com.youthexpedition.azit.modules.member.application.port.in.dto.LinkedProviderResponse;
import com.youthexpedition.azit.modules.member.application.port.in.dto.MyCrewResponse;
import com.youthexpedition.azit.modules.member.application.port.in.dto.MyInfoResponse;
import com.youthexpedition.azit.modules.member.application.port.out.LoadMemberPort;
import com.youthexpedition.azit.modules.member.application.port.out.LoadMemberSocialAccountPort;
import com.youthexpedition.azit.modules.member.application.port.out.LoadTermsVersionPort;
import com.youthexpedition.azit.modules.member.application.port.out.SaveMemberPort;
import com.youthexpedition.azit.modules.member.application.port.out.SaveMemberSocialAccountPort;
import com.youthexpedition.azit.modules.member.application.port.out.SaveMemberTermsConsentPort;
import com.youthexpedition.azit.modules.member.application.service.mapper.MemberResponseMapper;
import com.youthexpedition.azit.modules.member.domain.model.Member;
import com.youthexpedition.azit.modules.member.domain.model.MemberSocialAccount;
import com.youthexpedition.azit.modules.member.domain.model.MemberTermsConsent;
import com.youthexpedition.azit.modules.member.domain.model.MemberTermsConsentHistory;
import com.youthexpedition.azit.modules.member.domain.model.TermsVersion;
import com.youthexpedition.azit.modules.member.domain.model.enums.MemberErrorCode;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService implements MemberUseCase {
    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;
    private final LoadMemberSocialAccountPort loadMemberSocialAccountPort;
    private final SaveMemberSocialAccountPort saveMemberSocialAccountPort;
    private final SaveCrewMemberPort saveCrewMemberPort;
    private final LoadCrewMemberPort loadCrewMemberPort;
    private final LoadCrewPort loadCrewPort;
    private final SaveCrewPort saveCrewPort;
    private final TokenPort tokenPort;
    private final MemberResponseMapper memberResponseMapper;
    private final ImageUpdateUtil imageUpdateUtil;
    private final LoadTermsVersionPort loadTermsVersionPort;
    private final SaveMemberTermsConsentPort saveMemberTermsConsentPort;

    private static final String BLACKLIST_REASON_WITHDRAWN = "withdrawn";

    @Override
    @Transactional
    public void agreeToTerms(Long memberId, AgreeToTermsCommand command) {
        command.validateRequired(); // 필수 약관 동의 여부 검증

        Member member = getMember(memberId);
        member.completeTermsAgreement(command.marketingTermsAgreed(), command.notificationTermsAgreed()); // 멤버 상태 업데이트 (약관 동의)
        saveMemberPort.save(member);

        List<TermsVersion> latestVersions = loadTermsVersionPort.findAllLatest();
        Set<Long> alreadyConsentedVersionIds = loadTermsVersionPort.findConsentedVersionIdsByMemberId(memberId);
        LocalDateTime now = LocalDateTime.now();

        // 신규 버전: INSERT
        List<MemberTermsConsent> newConsents = latestVersions.stream()
                .filter(v -> command.isAgreed(v.getTermsType()))
                .filter(v -> !alreadyConsentedVersionIds.contains(v.getId()))
                .map(v -> MemberTermsConsent.agree(memberId, v.getId()))
                .toList();
        saveMemberTermsConsentPort.saveAll(newConsents);

        // 기존 동의 버전 재동의: agreed_at, updated_at 갱신
        Set<Long> reAgreedVersionIds = latestVersions.stream()
                .filter(v -> command.isAgreed(v.getTermsType()))
                .map(TermsVersion::getId)
                .filter(alreadyConsentedVersionIds::contains)
                .collect(Collectors.toSet());
        if (!reAgreedVersionIds.isEmpty()) {
            saveMemberTermsConsentPort.updateAgreedAt(memberId, reAgreedVersionIds, now);
        }

        // 동의 철회: 기존에 동의했지만 이번 요청에서 미동의한 버전은 현재 상태 테이블에서 삭제
        Set<Long> withdrawnVersionIds = latestVersions.stream()
                .filter(v -> !command.isAgreed(v.getTermsType()))
                .map(TermsVersion::getId)
                .filter(alreadyConsentedVersionIds::contains)
                .collect(Collectors.toSet());
        if (!withdrawnVersionIds.isEmpty()) {
            saveMemberTermsConsentPort.deleteByMemberIdAndVersionIds(memberId, withdrawnVersionIds);
        }

        // 이력: 동의/미동의 여부와 관계없이 모든 최신 약관에 대해 저장
        List<MemberTermsConsentHistory> histories = latestVersions.stream()
                .map(v -> MemberTermsConsentHistory.create(memberId, v.getId(), command.isAgreed(v.getTermsType())))
                .toList();
        saveMemberTermsConsentPort.saveAllHistory(histories);
    }


    @Override
    @Transactional
    public void withdraw(Long memberId, String accessToken) {
        Member member = getMember(memberId);

        // 이미 탈퇴한 회원인지 확인
        member.validateNotWithdrawn();

        processWithdrawal(member);

        tokenPort.addToBlacklist(accessToken, BLACKLIST_REASON_WITHDRAWN); // 블랙리스트에 액세스 토큰 추가
    }

    @Override
    @Transactional
    public void handleSocialAccountRevoked(String socialProviderId, SocialProvider socialProvider) {
        MemberSocialAccount revokedAccount = loadMemberSocialAccountPort
                .findBySocialInfo(socialProvider, socialProviderId)
                .orElse(null);

        // 이미 연동이 해제된 계정이면 무시 (서비스에서 해제 후 수신한 웹훅 또는 중복 수신)
        if (revokedAccount == null) {
            log.info("[MEMBER] 연동되어 있지 않은 {} 계정의 연동 해제 알림을 무시합니다.", socialProvider);
            return;
        }

        Member member = getMember(revokedAccount.getMemberId());

        // 이미 탈퇴한 회원이면 무시 (웹훅 중복 수신 대비)
        if (member.isWithdrawn()) {
            log.info("[MEMBER] 이미 탈퇴한 회원(memberId: {})에 대한 연동 해제 알림을 무시합니다.", member.getId());
            return;
        }

        int linkedCount = loadMemberSocialAccountPort.findAllByMemberId(member.getId()).size();

        // 남은 연동이 있으면 해당 소셜 계정만 연동 해제 (프로필·활동 데이터는 그대로 유지)
        if (linkedCount > 1) {
            saveMemberSocialAccountPort.deleteById(revokedAccount.getId());
            log.info("[MEMBER] memberId: {}의 {} 연동이 해제되었습니다. (남은 소셜 연동 개수 {}개)",
                    member.getId(), socialProvider, linkedCount - 1);
            return;
        }

        // 마지막 연동이 해제되면 로그인 수단이 사라지므로 탈퇴 처리
        log.info("[MEMBER] memberId: {}의 마지막 연동({})이 해제되어 탈퇴 처리합니다.", member.getId(), socialProvider);
        processWithdrawal(member);
    }

    private void processWithdrawal(Member member) {
        List<CrewMember> activeCrewMembers = loadCrewMemberPort.findAllActiveByMemberId(member.getId());

        // 탈퇴 가능한지 확인
        validateWithdrawal(member.getId(), activeCrewMembers);

        // 가입한 크루 인원 수 차감 및 상태 변경
        processCrewWithdrawal(member.getId(), activeCrewMembers);

        // 탈퇴 상태로 변경
        member.withdraw(LocalDateTime.now());

        tokenPort.deleteByMemberId(member.getId()); // 리프레시 토큰 삭제
        saveMemberPort.save(member);
    }

    @Override
    @Transactional
    public void updateEmailSharingStatus(String socialProviderId, SocialProvider socialProvider, boolean isEnabled) {
        MemberSocialAccount socialAccount = loadMemberSocialAccountPort
                .findBySocialInfo(socialProvider, socialProviderId)
                .orElse(null);

        // 이미 연동이 해제된 계정이면 무시
        if (socialAccount == null) {
            log.info("[MEMBER] 연동되어 있지 않은 {} 계정의 이메일 공유 상태 변경 알림을 무시합니다.", socialProvider);
            return;
        }

        Member member = getMember(socialAccount.getMemberId());

        // 탈퇴한 회원은 갱신하지 않음
        if (member.isWithdrawn()) {
            log.info("[MEMBER] 탈퇴한 회원(memberId: {})의 이메일 공유 상태 변경 요청을 무시합니다.", member.getId());
            return;
        }

        // 이메일 공유 상태는 플랫폼마다 다르므로 해당 소셜 계정에만 반영
        socialAccount.updateEmailSharingStatus(isEnabled);
        saveMemberSocialAccountPort.save(socialAccount);
    }

    @Override
    public MyInfoResponse getMyInfo(Long memberId) {
        Member member = getMember(memberId);
        return memberResponseMapper.toMyInfoResponse(member);
    }

    @Override
    public List<MyCrewResponse> getMyCrews(Long memberId) {
        List<CrewMember> activeCrewMembers = loadCrewMemberPort.findAllActiveByMemberId(memberId);

        if (activeCrewMembers.isEmpty()) return List.of();

        List<Long> crewIds = activeCrewMembers.stream().map(CrewMember::getCrewId).toList();
        Map<Long, Crew> crewMap = loadCrewPort.findAllByIds(crewIds).stream()
                .collect(Collectors.toMap(Crew::getId, crew -> crew));

        return activeCrewMembers.stream()
                .map(cm -> memberResponseMapper.toMyCrewResponse(cm, crewMap.get(cm.getCrewId())))
                .toList();
    }

    @Override
    public LinkedProviderResponse getLinkedProviders(Long memberId) {
        List<SocialProvider> providers = loadMemberSocialAccountPort.findAllByMemberId(memberId).stream()
                .map(MemberSocialAccount::getSocialProvider)
                .toList();
        return LinkedProviderResponse.of(providers);
    }

    @Override
    @Transactional
    public void updateMemberProfile(Long memberId, UpdateMemberProfileCommand command) {
        Member member = getMember(memberId);

        // 닉네임 업데이트
        member.updateNickname(command.nickname());

        // 이미지 업데이트
        imageUpdateUtil.update(command.imageUrl(), member.getProfileImageUrl(), memberId, true, member::updateProfileImageUrl);

        saveMemberPort.save(member);
    }

    private Member getMember(Long memberId) {
        return loadMemberPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    private void processCrewWithdrawal(Long memberId, List<CrewMember> crewMembers) {
        if (crewMembers.isEmpty()) return;

        // 가입 상태인 크루 ID 추출
        List<Long> joinedCrewIds = crewMembers.stream()
                .filter(cm -> cm.getStatus() == CrewMemberStatus.JOINED)
                .map(CrewMember::getCrewId)
                .toList();

        // 인원수 일괄 차감
        if (!joinedCrewIds.isEmpty()) {
            log.info("[MEMBER] memberId: {}, crewIds : {} 탈퇴하여 인원수가 차감됩니다.", memberId, joinedCrewIds);
            saveCrewPort.decrementMemberCountBatch(joinedCrewIds);
        }

        // 가입 완료 상태는 탈퇴(EXITED)로, 가입 신청 상태는 신청 취소(CANCELLED)로 변경 및 저장
        LocalDateTime now = LocalDateTime.now();
        crewMembers.forEach(cm -> {
            if (cm.getStatus() == CrewMemberStatus.JOINED) {
                cm.exit(now);
            } else if (cm.getStatus() == CrewMemberStatus.REQUESTED) {
                cm.cancel(now);
            }
        });
        saveCrewMemberPort.saveAll(crewMembers);
    }

    // 본인이 리더인 크루가 있으면 앱 탈퇴 불가
    private void validateWithdrawal(Long memberId, List<CrewMember> activeCrewMembers) {
        // 사용자가 JOINED 상태이면서 리더인 크루 조회
        List<CrewMember> crewMembersAsLeader = activeCrewMembers.stream()
                .filter(cm -> cm.getStatus() == CrewMemberStatus.JOINED)
                .filter(cm -> cm.getRole() == CrewMemberRole.LEADER)
                .toList();

        if (crewMembersAsLeader.isEmpty()) return;

        List<Long> crewIds = crewMembersAsLeader.stream()
                .map(CrewMember::getCrewId)
                .toList();

        log.warn("[MEMBER] memberId: {}, 리더로서 가입되어 있는 크루(crewIds: {})가 있어 앱 탈퇴가 불가능합니다.", memberId, crewIds);
        throw new BusinessException(CrewErrorCode.CANNOT_SERVICE_WITHDRAW_AS_LEADER);
    }

}
