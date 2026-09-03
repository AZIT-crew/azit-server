package com.youthexpedition.azit.modules.member.adapter.out.persistence.repository;

import com.youthexpedition.azit.modules.member.adapter.out.persistence.entity.MemberCrewNotificationSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberCrewNotificationSettingRepository extends JpaRepository<MemberCrewNotificationSettingEntity, Long> {
    List<MemberCrewNotificationSettingEntity> findAllByMemberId(Long memberId);
    Optional<MemberCrewNotificationSettingEntity> findByMemberIdAndCrewId(Long memberId, Long crewId);

    /*
     * flushAutomatically = true 가 반드시 필요함.
     * 크루 탈퇴·방출·해산 흐름에서 crew_member 상태를 변경한 직후에 호출되는데, 대상 테이블이 달라
     * Hibernate 오토 플러시가 걸리지 않음. flush 없이 clearAutomatically 로 영속성 컨텍스트를 비우면
     * 아직 flush되지 않은 crew_member 변경분이 통째로 유실됨.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MemberCrewNotificationSettingEntity s WHERE s.memberId = :memberId AND s.crewId = :crewId")
    void deleteByMemberIdAndCrewId(@Param("memberId") Long memberId, @Param("crewId") Long crewId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MemberCrewNotificationSettingEntity s WHERE s.crewId = :crewId")
    void deleteByCrewId(@Param("crewId") Long crewId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MemberCrewNotificationSettingEntity s WHERE s.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);
}
