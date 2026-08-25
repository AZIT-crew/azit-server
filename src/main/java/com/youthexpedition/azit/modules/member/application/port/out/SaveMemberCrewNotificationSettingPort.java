package com.youthexpedition.azit.modules.member.application.port.out;

import com.youthexpedition.azit.modules.member.domain.model.MemberCrewNotificationSetting;

import java.util.List;

public interface SaveMemberCrewNotificationSettingPort {
    void save(MemberCrewNotificationSetting setting);
    void saveAll(List<MemberCrewNotificationSetting> settings);
    // 크루를 떠나면 설정을 삭제한다 (재가입 시 기본값으로 초기화)
    void deleteByMemberIdAndCrewId(Long memberId, Long crewId);
    void deleteByCrewId(Long crewId);
    void deleteByMemberId(Long memberId);
}
