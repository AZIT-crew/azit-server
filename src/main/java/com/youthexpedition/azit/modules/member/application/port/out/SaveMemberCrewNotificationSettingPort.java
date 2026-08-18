package com.youthexpedition.azit.modules.member.application.port.out;

import com.youthexpedition.azit.modules.member.domain.model.MemberCrewNotificationSetting;

import java.util.List;

public interface SaveMemberCrewNotificationSettingPort {
    void save(MemberCrewNotificationSetting setting);
    void saveAll(List<MemberCrewNotificationSetting> settings);
}
