package com.youthexpedition.azit.modules.notification.application.port.out;

import com.youthexpedition.azit.modules.notification.domain.model.DeviceToken;

import java.util.List;
import java.util.Optional;

public interface LoadDeviceTokenPort {
    Optional<DeviceToken> findByToken(String token);
    List<DeviceToken> findAllByMemberIds(List<Long> memberIds);
}
