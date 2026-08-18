package com.youthexpedition.azit.modules.member.adapter.out.persistence;

import com.youthexpedition.azit.modules.member.adapter.out.mapper.MemberCrewNotificationSettingMapper;
import com.youthexpedition.azit.modules.member.adapter.out.persistence.entity.MemberCrewNotificationSettingEntity;
import com.youthexpedition.azit.modules.member.adapter.out.persistence.repository.MemberCrewNotificationSettingRepository;
import com.youthexpedition.azit.modules.member.application.port.out.LoadMemberCrewNotificationSettingPort;
import com.youthexpedition.azit.modules.member.application.port.out.SaveMemberCrewNotificationSettingPort;
import com.youthexpedition.azit.modules.member.domain.model.MemberCrewNotificationSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MemberCrewNotificationSettingPersistenceAdapter
        implements LoadMemberCrewNotificationSettingPort, SaveMemberCrewNotificationSettingPort {

    private final MemberCrewNotificationSettingRepository memberCrewNotificationSettingRepository;
    private final MemberCrewNotificationSettingMapper memberCrewNotificationSettingMapper;

    @Override
    public List<MemberCrewNotificationSetting> findAllByMemberId(Long memberId) {
        return memberCrewNotificationSettingRepository.findAllByMemberId(memberId).stream()
                .map(memberCrewNotificationSettingMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<MemberCrewNotificationSetting> findByMemberIdAndCrewId(Long memberId, Long crewId) {
        return memberCrewNotificationSettingRepository.findByMemberIdAndCrewId(memberId, crewId)
                .map(memberCrewNotificationSettingMapper::toDomain);
    }

    @Override
    public void save(MemberCrewNotificationSetting setting) {
        memberCrewNotificationSettingRepository.save(memberCrewNotificationSettingMapper.toEntity(setting));
    }

    @Override
    public void saveAll(List<MemberCrewNotificationSetting> settings) {
        List<MemberCrewNotificationSettingEntity> entities = settings.stream()
                .map(memberCrewNotificationSettingMapper::toEntity)
                .toList();
        memberCrewNotificationSettingRepository.saveAll(entities);
    }
}
