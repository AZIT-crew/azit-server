package com.youthexpedition.azit.modules.notification.application.port.in;

import com.youthexpedition.azit.modules.notification.application.port.in.command.PushDispatchCommand;

public interface DispatchPushUseCase {
    void dispatch(PushDispatchCommand command);
}
