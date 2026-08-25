package com.youthexpedition.azit.modules.member.adapter.out.mapper;

import com.youthexpedition.azit.modules.member.adapter.out.persistence.entity.MemberCrewNotificationSettingEntity;
import com.youthexpedition.azit.modules.member.domain.model.MemberCrewNotificationSetting;
import org.springframework.stereotype.Component;

@Component
public class MemberCrewNotificationSettingMapper {

    public MemberCrewNotificationSetting toDomain(MemberCrewNotificationSettingEntity entity) {
        return MemberCrewNotificationSetting.builder()
                .id(entity.getId())
                .memberId(entity.getMemberId())
                .crewId(entity.getCrewId())
                .isRegularRunEnabled(entity.isRegularRunEnabled())
                .isLightningRunEnabled(entity.isLightningRunEnabled())
                .build();
    }

    public MemberCrewNotificationSettingEntity toEntity(MemberCrewNotificationSetting domain) {
        return MemberCrewNotificationSettingEntity.builder()
                .id(domain.getId())
                .memberId(domain.getMemberId())
                .crewId(domain.getCrewId())
                .isRegularRunEnabled(domain.isRegularRunEnabled())
                .isLightningRunEnabled(domain.isLightningRunEnabled())
                .build();
    }
}
