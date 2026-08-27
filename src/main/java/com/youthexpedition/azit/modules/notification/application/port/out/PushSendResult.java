package com.youthexpedition.azit.modules.notification.application.port.out;

import java.util.List;

/**
 * @param successCount  발송 성공 건수
 * @param invalidTokens 더 이상 유효하지 않아 삭제해야 하는 토큰 (앱 삭제·재설치 등)
 */
public record PushSendResult(
        int successCount,
        int failureCount,
        List<String> invalidTokens
) {
    public static PushSendResult of(int successCount, int failureCount, List<String> invalidTokens) {
        return new PushSendResult(successCount, failureCount, invalidTokens);
    }

    public static PushSendResult empty() {
        return new PushSendResult(0, 0, List.of());
    }
}
