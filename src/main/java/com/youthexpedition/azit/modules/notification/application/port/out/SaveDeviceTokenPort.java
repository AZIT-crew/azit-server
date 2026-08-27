package com.youthexpedition.azit.modules.notification.application.port.out;

import com.youthexpedition.azit.modules.notification.domain.model.DeviceToken;

import java.util.List;

public interface SaveDeviceTokenPort {
    void save(DeviceToken deviceToken);
    void deleteByToken(String token);
    void deleteAllByTokens(List<String> tokens);
}
