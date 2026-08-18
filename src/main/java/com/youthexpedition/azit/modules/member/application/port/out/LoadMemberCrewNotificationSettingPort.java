package com.youthexpedition.azit.modules.member.application.port.out;

import com.youthexpedition.azit.modules.member.domain.model.MemberCrewNotificationSetting;

import java.util.List;
import java.util.Optional;

public interface LoadMemberCrewNotificationSettingPort {
    List<MemberCrewNotificationSetting> findAllByMemberId(Long memberId);
    Optional<MemberCrewNotificationSetting> findByMemberIdAndCrewId(Long memberId, Long crewId);
}
