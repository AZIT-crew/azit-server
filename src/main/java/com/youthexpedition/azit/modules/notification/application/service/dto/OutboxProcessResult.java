package com.youthexpedition.azit.modules.notification.application.service.dto;

import com.youthexpedition.azit.modules.notification.application.port.in.command.PushDispatchCommand;

/**
 * 아웃박스 한 건의 처리 결과.
 * 처리할 건이 없는 경우와, 처리했지만 푸시 대상이 없는 경우(전체 알림 꺼짐·처리 실패)를 구분하기 위함.
 */
public record OutboxProcessResult(
        boolean processed,
        PushDispatchCommand pushTarget
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
