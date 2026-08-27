package com.youthexpedition.azit.modules.notification.application.port.in;

import com.youthexpedition.azit.modules.notification.application.port.in.command.PushDispatchCommand;

import java.util.List;

public interface ProcessNotificationOutboxUseCase {
    // 대기 중인 아웃박스를 선점해 인앱 알림을 생성하고, 푸시 발송 대상을 반환함
    List<PushDispatchCommand> processPending();
}
