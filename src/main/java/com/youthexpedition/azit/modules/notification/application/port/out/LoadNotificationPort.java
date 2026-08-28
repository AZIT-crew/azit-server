package com.youthexpedition.azit.modules.notification.application.port.out;

public interface LoadNotificationPort {
    // iOS 배지에 표시할 안 읽은 알림 개수
    long countUnreadByReceiverId(Long receiverId);
}
