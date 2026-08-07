package com.youthexpedition.azit.modules.member.domain.model;

import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 회원에 연동된 소셜 계정. 회원 1명이 여러 플랫폼(카카오/애플)을 동시에 연동할 수 있다.
 * 이메일과 애플 리프레시 토큰은 플랫폼마다 값이 다르므로 Member가 아닌 이곳에서 관리한다.
 */
@Getter
@Builder
@AllArgsConstructor
public class MemberSocialAccount {
    private final Long id;
    private final Long memberId;
    private final SocialProvider socialProvider;
    private final String socialProviderId;
    private String email; // 해당 소셜 계정에서 받은 이메일
    private boolean isEmailSharingEnabled; // 플랫폼별 이메일 공유 상태
    private String appleRefreshToken; // 애플 연동 해제용 토큰
    private final LocalDateTime linkedAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MemberSocialAccount link(Long memberId, SocialProvider socialProvider, String socialProviderId,
                                           String email, boolean isEmailSharingEnabled, String appleRefreshToken) {
        return MemberSocialAccount.builder()
                .memberId(memberId)
                .socialProvider(socialProvider)
                .socialProviderId(socialProviderId)
                .email(email)
                .isEmailSharingEnabled(isEmailSharingEnabled)
                .appleRefreshToken(appleRefreshToken)
                .linkedAt(LocalDateTime.now())
                .build();
    }

    // 애플 리프레시 토큰 업데이트
    public void updateAppleRefreshToken(String appleRefreshToken) {
        if (this.socialProvider == SocialProvider.APPLE) {
            this.appleRefreshToken = appleRefreshToken;
        }
    }

    public void updateEmailSharingStatus(boolean isEnabled) {
        this.isEmailSharingEnabled = isEnabled;
    }
}
