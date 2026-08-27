package com.youthexpedition.azit.modules.notification.adapter.out.mapper;

import com.youthexpedition.azit.modules.notification.adapter.out.persistence.entity.DeviceTokenEntity;
import com.youthexpedition.azit.modules.notification.domain.model.DeviceToken;
import org.springframework.stereotype.Component;

@Component
public class DeviceTokenMapper {

    public DeviceToken toDomain(DeviceTokenEntity entity) {
        return DeviceToken.builder()
                .id(entity.getId())
                .memberId(entity.getMemberId())
                .token(entity.getToken())
                .deviceType(entity.getDeviceType())
                .build();
    }

    public DeviceTokenEntity toEntity(DeviceToken domain) {
        return DeviceTokenEntity.builder()
                .id(domain.getId())
                .memberId(domain.getMemberId())
                .token(domain.getToken())
                .deviceType(domain.getDeviceType())
                .build();
    }
}
