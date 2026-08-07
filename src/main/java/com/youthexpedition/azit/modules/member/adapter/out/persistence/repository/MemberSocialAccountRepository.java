package com.youthexpedition.azit.modules.member.adapter.out.persistence.repository;

import com.youthexpedition.azit.modules.member.adapter.out.persistence.entity.MemberSocialAccountEntity;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberSocialAccountRepository extends JpaRepository<MemberSocialAccountEntity, Long> {

    Optional<MemberSocialAccountEntity> findBySocialProviderAndSocialProviderId(SocialProvider socialProvider, String socialProviderId);

    List<MemberSocialAccountEntity> findAllByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);
}
