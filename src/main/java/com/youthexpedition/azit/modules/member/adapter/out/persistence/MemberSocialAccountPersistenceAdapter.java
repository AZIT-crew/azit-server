package com.youthexpedition.azit.modules.member.adapter.out.persistence;

import com.youthexpedition.azit.modules.member.adapter.out.mapper.MemberSocialAccountMapper;
import com.youthexpedition.azit.modules.member.adapter.out.persistence.entity.MemberSocialAccountEntity;
import com.youthexpedition.azit.modules.member.adapter.out.persistence.repository.MemberSocialAccountRepository;
import com.youthexpedition.azit.modules.member.application.port.out.LoadMemberSocialAccountPort;
import com.youthexpedition.azit.modules.member.application.port.out.SaveMemberSocialAccountPort;
import com.youthexpedition.azit.modules.member.domain.model.MemberSocialAccount;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MemberSocialAccountPersistenceAdapter implements LoadMemberSocialAccountPort, SaveMemberSocialAccountPort {
    private final MemberSocialAccountRepository memberSocialAccountRepository;
    private final MemberSocialAccountMapper memberSocialAccountMapper;

    @Override
    public Optional<MemberSocialAccount> findBySocialInfo(SocialProvider socialProvider, String socialProviderId) {
        return memberSocialAccountRepository.findBySocialProviderAndSocialProviderId(socialProvider, socialProviderId)
                .map(memberSocialAccountMapper::toDomain);
    }

    @Override
    public List<MemberSocialAccount> findAllByMemberId(Long memberId) {
        return memberSocialAccountRepository.findAllByMemberId(memberId).stream()
                .map(memberSocialAccountMapper::toDomain)
                .toList();
    }

    /**
     * saveAndFlush를 쓰는 이유: 연동 시 유니크 제약(social_provider, social_provider_id) 위반을
     * 커밋 시점이 아닌 호출 시점에 DataIntegrityViolationException으로 드러내기 위함.
     * 서비스가 이를 잡아 "이미 다른 계정에 연동됨" 응답으로 변환한다.
     */
    @Override
    public MemberSocialAccount save(MemberSocialAccount socialAccount) {
        MemberSocialAccountEntity entity = memberSocialAccountMapper.toEntity(socialAccount);
        MemberSocialAccountEntity savedEntity = memberSocialAccountRepository.saveAndFlush(entity);
        return memberSocialAccountMapper.toDomain(savedEntity);
    }

    @Override
    public void deleteById(Long id) {
        memberSocialAccountRepository.deleteById(id);
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        memberSocialAccountRepository.deleteByMemberId(memberId);
    }
}
