package com.youthexpedition.azit.modules.auth.application.service;

import com.youthexpedition.azit.modules.auth.adapter.in.web.dto.AppleNotificationPayload;
import com.youthexpedition.azit.modules.auth.adapter.out.external.AppleAuthAdapter;
import com.youthexpedition.azit.modules.member.application.port.in.MemberUseCase;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppleNotificationService 단위 테스트")
class AppleNotificationServiceTest {

    @Mock private AppleAuthAdapter appleAuthAdapter;
    @Mock private MemberUseCase memberUseCase;

    @InjectMocks
    private AppleNotificationService appleNotificationService;

    private static final String PAYLOAD = "payload";
    private static final String APPLE_SUB = "appleSub";

    private void stubEvent(String type) {
        doReturn(new AppleNotificationPayload.Event(type, APPLE_SUB, 0L))
                .when(appleAuthAdapter).parseNotification(PAYLOAD);
    }

    @Test
    @DisplayName("성공 - 연동 해제(consent-revoked) 수신 시 탈퇴가 아니라 연동 해제 처리로 위임")
    void handleNotification_delegatesToRevokeHandler_whenConsentRevoked() {
        // given
        stubEvent("consent-revoked");

        // when
        appleNotificationService.handleNotification(PAYLOAD);

        // then
        verify(memberUseCase).handleSocialAccountRevoked(APPLE_SUB, SocialProvider.APPLE);
        verify(memberUseCase, never()).updateEmailSharingStatus(anyString(), any(), anyBoolean());
    }

    @Test
    @DisplayName("성공 - Apple ID 삭제(account-delete)도 동일하게 연동 해제 처리로 위임")
    void handleNotification_delegatesToRevokeHandler_whenAccountDelete() {
        // given
        stubEvent("account-delete");

        // when
        appleNotificationService.handleNotification(PAYLOAD);

        // then
        verify(memberUseCase).handleSocialAccountRevoked(APPLE_SUB, SocialProvider.APPLE);
    }

    @Test
    @DisplayName("성공 - 이메일 공유 활성화 수신 시 해당 소셜 계정 상태만 갱신")
    void handleNotification_updatesEmailSharing_whenEmailEnabled() {
        // given
        stubEvent("email-enabled");

        // when
        appleNotificationService.handleNotification(PAYLOAD);

        // then
        verify(memberUseCase).updateEmailSharingStatus(APPLE_SUB, SocialProvider.APPLE, true);
        verify(memberUseCase, never()).handleSocialAccountRevoked(anyString(), any());
    }

    @Test
    @DisplayName("성공 - 이메일 공유 비활성화 수신 시 해당 소셜 계정 상태만 갱신")
    void handleNotification_updatesEmailSharing_whenEmailDisabled() {
        // given
        stubEvent("email-disabled");

        // when
        appleNotificationService.handleNotification(PAYLOAD);

        // then
        verify(memberUseCase).updateEmailSharingStatus(APPLE_SUB, SocialProvider.APPLE, false);
    }

    @Test
    @DisplayName("성공 - 알 수 없는 타입은 아무 처리도 하지 않음")
    void handleNotification_doesNothing_whenUnknownType() {
        // given
        stubEvent("unknown-type");

        // when
        appleNotificationService.handleNotification(PAYLOAD);

        // then
        verifyNoInteractions(memberUseCase);
    }
}
