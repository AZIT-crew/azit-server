package com.youthexpedition.azit.modules.member.adapter.out.persistence.repository;

import com.youthexpedition.azit.modules.member.adapter.out.persistence.entity.MemberTermsConsentHistoryEntity;
import com.youthexpedition.azit.modules.member.domain.model.enums.TermsType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberTermsConsentHistoryRepository extends JpaRepository<MemberTermsConsentHistoryEntity, Long> {

    // 특정 약관 종류의 가장 최근 이력 1건 (버전이 올라가도 종류 기준으로 조회)
    @Query("""
            SELECT h FROM MemberTermsConsentHistoryEntity h
            JOIN TermsVersionEntity tv ON tv.id = h.termsVersionId
            WHERE h.memberId = :memberId AND tv.termsType = :termsType
            ORDER BY h.id DESC
            """)
    List<MemberTermsConsentHistoryEntity> findLatestByMemberIdAndTermsType(@Param("memberId") Long memberId,
                                                                          @Param("termsType") TermsType termsType,
                                                                          Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MemberTermsConsentHistoryEntity h WHERE h.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);
}
