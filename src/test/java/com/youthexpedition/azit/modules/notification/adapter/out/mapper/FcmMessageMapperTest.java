package com.youthexpedition.azit.modules.notification.adapter.out.mapper;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import com.youthexpedition.azit.modules.notification.application.port.out.PushSendResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("FcmMessageMapper 단위 테스트")
class FcmMessageMapperTest {

    private static final List<String> TOKENS = List.of("token-0", "token-1", "token-2");

    private final FcmMessageMapper fcmMessageMapper = new FcmMessageMapper();

    @Test
    @DisplayName("성공: 모두 발송에 성공하면 삭제할 토큰이 없다.")
    void toResult_success_returnsNoInvalidToken() {
        // given
        BatchResponse response = batchResponse(3, 0, success(), success(), success());

        // when
        PushSendResult result = fcmMessageMapper.toResult(response, TOKENS);

        // then
        assertThat(result.successCount()).isEqualTo(3);
        assertThat(result.invalidTokens()).isEmpty();
    }

    @Test
    @DisplayName("성공: 실패한 응답의 순서에 맞는 토큰만 삭제 대상으로 골라낸다.")
    void toResult_success_matchesInvalidTokenByResponseOrder() {
        // given - 두 번째 토큰만 무효
        BatchResponse response = batchResponse(2, 1,
                success(), failure(MessagingErrorCode.UNREGISTERED), success());

        // when
        PushSendResult result = fcmMessageMapper.toResult(response, TOKENS);

        // then - 응답 순서와 토큰 순서가 어긋나면 엉뚱한 토큰이 지워진다
        assertThat(result.invalidTokens()).containsExactly("token-1");
        assertThat(result.failureCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("성공: 등록 해제와 잘못된 토큰은 모두 삭제 대상이다.")
    void toResult_success_collectsAllInvalidTokenErrors() {
        // given
        BatchResponse response = batchResponse(1, 2,
                failure(MessagingErrorCode.UNREGISTERED), failure(MessagingErrorCode.INVALID_ARGUMENT), success());

        // when
        PushSendResult result = fcmMessageMapper.toResult(response, TOKENS);

        // then
        assertThat(result.invalidTokens()).containsExactly("token-0", "token-1");
    }

    @Test
    @DisplayName("성공: 일시적인 오류로 실패한 토큰은 삭제하지 않는다.")
    void toResult_keepsToken_whenFailureIsRetryable() {
        // given - 서버 일시 장애. 토큰 자체는 유효하다
        BatchResponse response = batchResponse(2, 1,
                success(), failure(MessagingErrorCode.UNAVAILABLE), success());

        // when
        PushSendResult result = fcmMessageMapper.toResult(response, TOKENS);

        // then
        assertThat(result.invalidTokens()).isEmpty();
        assertThat(result.failureCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("성공: 예외 정보가 없는 실패 응답은 삭제하지 않는다.")
    void toResult_keepsToken_whenExceptionIsMissing() {
        // given
        SendResponse unknownFailure = mock(SendResponse.class);
        when(unknownFailure.isSuccessful()).thenReturn(false);
        when(unknownFailure.getException()).thenReturn(null);
        BatchResponse response = batchResponse(2, 1, success(), unknownFailure, success());

        // when
        PushSendResult result = fcmMessageMapper.toResult(response, TOKENS);

        // then
        assertThat(result.invalidTokens()).isEmpty();
    }

    private BatchResponse batchResponse(int successCount, int failureCount, SendResponse... responses) {
        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getResponses()).thenReturn(List.of(responses));
        when(batchResponse.getSuccessCount()).thenReturn(successCount);
        when(batchResponse.getFailureCount()).thenReturn(failureCount);
        return batchResponse;
    }

    private SendResponse success() {
        SendResponse sendResponse = mock(SendResponse.class);
        when(sendResponse.isSuccessful()).thenReturn(true);
        return sendResponse;
    }

    private SendResponse failure(MessagingErrorCode errorCode) {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(errorCode);

        SendResponse sendResponse = mock(SendResponse.class);
        when(sendResponse.isSuccessful()).thenReturn(false);
        when(sendResponse.getException()).thenReturn(exception);
        return sendResponse;
    }
}
