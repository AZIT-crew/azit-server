package com.youthexpedition.azit.modules.member.adapter.out.persistence.repository;

import com.youthexpedition.azit.modules.member.adapter.out.persistence.entity.MemberCrewNotificationSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberCrewNotificationSettingRepository extends JpaRepository<MemberCrewNotificationSettingEntity, Long> {
    List<MemberCrewNotificationSettingEntity> findAllByMemberId(Long memberId);
    Optional<MemberCrewNotificationSettingEntity> findByMemberIdAndCrewId(Long memberId, Long crewId);
}
