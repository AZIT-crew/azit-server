package com.youthexpedition.azit.modules.notification.application.service.dto;

import com.youthexpedition.azit.modules.notification.application.port.in.command.PushDispatchCommand;

public record OutboxProcessResult(
        boolean processed, // 처리할 건이 없을 경우 false
        PushDispatchCommand pushTarget // 푸시할 대상 (전체 알림 꺼짐/처리 실패 구분)
) {
    public static OutboxProcessResult nothingToProcess() {
        return new OutboxProcessResult(false, null);
    }

    public static OutboxProcessResult processed(PushDispatchCommand pushTarget) {
        return new OutboxProcessResult(true, pushTarget);
    }

    public boolean hasPushTarget() {
        return pushTarget != null;
    }
}
