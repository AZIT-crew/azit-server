package com.youthexpedition.azit.modules.notification.adapter.out.persistence;

import com.youthexpedition.azit.modules.notification.adapter.out.mapper.DeviceTokenMapper;
import com.youthexpedition.azit.modules.notification.adapter.out.persistence.repository.DeviceTokenRepository;
import com.youthexpedition.azit.modules.notification.application.port.out.LoadDeviceTokenPort;
import com.youthexpedition.azit.modules.notification.application.port.out.SaveDeviceTokenPort;
import com.youthexpedition.azit.modules.notification.domain.model.DeviceToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DeviceTokenPersistenceAdapter implements LoadDeviceTokenPort, SaveDeviceTokenPort {

    private final DeviceTokenRepository deviceTokenRepository;
    private final DeviceTokenMapper deviceTokenMapper;

    @Override
    public Optional<DeviceToken> findByToken(String token) {
        return deviceTokenRepository.findByToken(token)
                .map(deviceTokenMapper::toDomain);
    }

    @Override
    public List<DeviceToken> findAllByMemberIds(List<Long> memberIds) {
        if (memberIds.isEmpty()) return List.of();

        return deviceTokenRepository.findAllByMemberIdIn(memberIds).stream()
                .map(deviceTokenMapper::toDomain)
                .toList();
    }

    @Override
    public void save(DeviceToken deviceToken) {
        deviceTokenRepository.save(deviceTokenMapper.toEntity(deviceToken));
    }

    @Override
    @Transactional
    public void deleteByToken(String token) {
        deviceTokenRepository.deleteByToken(token);
    }

    @Override
    @Transactional
    public void deleteAllByTokens(List<String> tokens) {
        if (tokens.isEmpty()) return;

        deviceTokenRepository.deleteAllByTokenIn(tokens);
    }
}
