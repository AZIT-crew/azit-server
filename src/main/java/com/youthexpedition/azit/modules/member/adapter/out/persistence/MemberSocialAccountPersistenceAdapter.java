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

    @Override
    public MemberSocialAccount save(MemberSocialAccount socialAccount) {
        MemberSocialAccountEntity entity = memberSocialAccountMapper.toEntity(socialAccount);
        MemberSocialAccountEntity savedEntity = memberSocialAccountRepository.save(entity);
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
