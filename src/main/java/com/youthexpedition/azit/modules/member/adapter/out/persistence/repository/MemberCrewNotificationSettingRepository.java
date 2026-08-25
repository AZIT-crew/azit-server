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

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MemberCrewNotificationSettingEntity s WHERE s.memberId = :memberId AND s.crewId = :crewId")
    void deleteByMemberIdAndCrewId(@Param("memberId") Long memberId, @Param("crewId") Long crewId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MemberCrewNotificationSettingEntity s WHERE s.crewId = :crewId")
    void deleteByCrewId(@Param("crewId") Long crewId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MemberCrewNotificationSettingEntity s WHERE s.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);
}
