package com.youthexpedition.azit.modules.notification.domain.model.enums;

import com.youthexpedition.azit.infrastructure.common.response.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {
    INVALID_NOTIFICATION_PAYLOAD("INVALID_NOTIFICATION_PAYLOAD", "알림 정보를 처리할 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    NOTIFICATION_RECEIVER_NOT_FOUND("NOTIFICATION_RECEIVER_NOT_FOUND", "알림 수신자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
