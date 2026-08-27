package com.youthexpedition.azit.modules.notification.application.port.out;

public interface PushSenderPort {
    PushSendResult send(PushMessage message);
}
