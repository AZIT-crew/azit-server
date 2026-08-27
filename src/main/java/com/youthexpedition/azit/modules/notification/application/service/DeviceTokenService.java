package com.youthexpedition.azit.modules.notification.application.service;

import com.youthexpedition.azit.modules.notification.application.port.in.DeviceTokenUseCase;
import com.youthexpedition.azit.modules.notification.application.port.in.command.RegisterDeviceTokenCommand;
import com.youthexpedition.azit.modules.notification.application.port.out.LoadDeviceTokenPort;
import com.youthexpedition.azit.modules.notification.application.port.out.SaveDeviceTokenPort;
import com.youthexpedition.azit.modules.notification.domain.model.DeviceToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceTokenService implements DeviceTokenUseCase {

    private final LoadDeviceTokenPort loadDeviceTokenPort;
    private final SaveDeviceTokenPort saveDeviceTokenPort;

    @Override
    @Transactional
    public void register(Long memberId, RegisterDeviceTokenCommand command) {
        // 같은 기기를 다른 계정이 사용하면 소유자만 바꿈 (이전 계정으로 알림이 가지 않도록)
        DeviceToken deviceToken = loadDeviceTokenPort.findByToken(command.token())
                .map(existing -> {
                    existing.changeOwner(memberId, command.deviceType());
                    return existing;
                })
                .orElseGet(() -> DeviceToken.register(memberId, command.token(), command.deviceType()));

        saveDeviceTokenPort.save(deviceToken);
        log.info("[NOTIFICATION] memberId: {} 의 기기 토큰이 등록되었습니다. deviceType: {}", memberId, command.deviceType());
    }

    @Override
    @Transactional
    public void delete(Long memberId, String token) {
        // 없거나 남의 토큰이면 조용히 무시함 (로그아웃은 실패시키지 않음)
        loadDeviceTokenPort.findByToken(token)
                .filter(deviceToken -> deviceToken.isOwnedBy(memberId))
                .ifPresent(deviceToken -> {
                    saveDeviceTokenPort.deleteByToken(token);
                    log.info("[NOTIFICATION] memberId: {} 의 기기 토큰이 삭제되었습니다.", memberId);
                });
    }
}
