package com.youthexpedition.azit.modules.member.adapter.out.persistence.repository;

import com.youthexpedition.azit.modules.member.adapter.out.persistence.entity.MemberSocialAccountEntity;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberSocialAccountRepository extends JpaRepository<MemberSocialAccountEntity, Long> {

    Optional<MemberSocialAccountEntity> findBySocialProviderAndSocialProviderId(SocialProvider socialProvider, String socialProviderId);

    List<MemberSocialAccountEntity> findAllByMemberId(Long memberId);

    /**
     * 연동 해제처럼 "마지막 하나" 판정 후 삭제까지 원자적으로 처리해야 하는 경우에 사용한다.
     * 동시 요청이 각각 다른 플랫폼을 해제해 연동이 0개가 되는 것을 막는다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from MemberSocialAccountEntity account where account.memberId = :memberId")
    List<MemberSocialAccountEntity> findAllByMemberIdForUpdate(@Param("memberId") Long memberId);

    void deleteByMemberId(Long memberId);
}
