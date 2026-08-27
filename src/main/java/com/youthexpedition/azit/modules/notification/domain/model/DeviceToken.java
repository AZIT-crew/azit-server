package com.youthexpedition.azit.modules.notification.domain.model;

import com.youthexpedition.azit.modules.notification.domain.model.enums.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 푸시 발송 대상 기기. 회원 한 명이 여러 기기를 가질 수 있음.
 */
@Getter
@Builder
@AllArgsConstructor
public class DeviceToken {
    private final Long id;
    private Long memberId;
    private final String token;
    private DeviceType deviceType;

    public static DeviceToken register(Long memberId, String token, DeviceType deviceType) {
        return DeviceToken.builder()
                .memberId(memberId)
                .token(token)
                .deviceType(deviceType)
                .build();
    }

    // 같은 기기를 다른 계정이 사용하면 소유자를 옮김 (이전 계정으로 알림이 가지 않도록)
    public void changeOwner(Long memberId, DeviceType deviceType) {
        this.memberId = memberId;
        this.deviceType = deviceType;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }
}
