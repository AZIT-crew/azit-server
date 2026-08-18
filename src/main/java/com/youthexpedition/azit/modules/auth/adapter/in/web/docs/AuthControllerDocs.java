package com.youthexpedition.azit.modules.auth.adapter.in.web.docs;

import com.youthexpedition.azit.infrastructure.common.annotation.CurrentAccessToken;
import com.youthexpedition.azit.infrastructure.common.annotation.CurrentMemberId;
import com.youthexpedition.azit.infrastructure.common.response.CommonResponse;
import com.youthexpedition.azit.infrastructure.config.swagger.ApiErrorCodeExamples;
import com.youthexpedition.azit.modules.auth.adapter.in.web.dto.AppleNotificationRequest;
import com.youthexpedition.azit.modules.auth.adapter.in.web.dto.CreateAppleLinkSessionRequest;
import com.youthexpedition.azit.modules.auth.adapter.in.web.dto.LinkSocialAccountRequest;
import com.youthexpedition.azit.modules.auth.adapter.in.web.dto.SocialLoginRequest;
import com.youthexpedition.azit.modules.auth.application.port.in.dto.AppleLinkSessionResponse;
import com.youthexpedition.azit.modules.auth.application.port.in.dto.SocialLoginResponse;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Tag(name = "Auth" , description = "사용자 인증 API")
public interface AuthControllerDocs {

    @Operation(
            summary = "소셜 로그인 (애플 제외)",
            description = """
            카카오 등 소셜 플랫폼의 인가 코드 및 액세스 토큰을 통해 로그인을 진행합니다. <br><br>
            
            **[참고 사항]** <br>
            * 보안을 위해 리프레시 토큰은 HttpOnly 쿠키에 저장되어 발급됩니다. <br>
            """
    )
    @ApiErrorCodeExamples({
            "INVALID_SOCIAL_CODE", "SOCIAL_AUTHENTICATION_FAILED", "INVALID_KAKAO_ACCESS_TOKEN", "MISSING_SOCIAL_CREDENTIAL", "WITHDRAWAL_GRACE_PERIOD_EXPIRED"
    })
    CommonResponse<SocialLoginResponse> socialLogin(
            @PathVariable SocialProvider provider, @Valid @RequestBody SocialLoginRequest request, HttpServletResponse response);

    @Operation(
            summary = "애플 로그인·연동 콜백 (백엔드 전용)",
            description = """
            애플 서버로부터 직접 리다이렉트되는 콜백 엔드포인트입니다. 클라이언트가 아닌 서버 간 통신을 통해 처리합니다. <br><br>

            **[state 값에 따른 분기]** <br>
            * 연동 세션 발급 API로 받은 state인 경우: 해당 회원에 애플 계정을 추가 연동한 뒤, 세션에 등록된 프론트 주소로 리다이렉트합니다.
              결과는 쿼리 파라미터로 전달됩니다. (성공: `?result=success`, 실패: `?result=fail&error=에러코드`) <br>
            * 그 외의 경우: 기존과 동일하게 로그인으로 처리하고 state에 담긴 프론트 주소로 리다이렉트합니다.
              허용되지 않은 주소이면 INVALID_REDIRECT_URL로 차단됩니다. <br>
            """
    )
    @ApiErrorCodeExamples({
            "INVALID_APPLE_ID_TOKEN", "APPLE_PUBLIC_KEY_NOT_FOUND", "APPLE_CLIENT_SECRET_CREATION_FAILED",
            "INVALID_REDIRECT_URL", "WITHDRAWAL_GRACE_PERIOD_EXPIRED"
    })
    void appleLogin(@RequestParam("code") String code, @RequestParam("id_token") String idToken,
                    @RequestParam(value = "user", required = false) String user,
                    @RequestParam(value = "state", required = false) String state, HttpServletResponse response) throws IOException;

    @Operation(
            summary = "토큰 재발급",
            description = """
            쿠키에 저장된 리프레시 토큰을 사용하여 액세스 토큰을 갱신합니다. <br><br>
            
            **[RTR(Refresh Token Rotation) 방식 적용]** <br>
            * 토큰 재발급 시 기존 리프레시 토큰은 폐기되고 새로운 리프레시 토큰이 쿠키에 다시 저장됩니다. <br>
            * 만약 이미 사용된 리프레시 토큰이 다시 제출될 경우, 보안 위협으로 간주하여 쿠키 삭제 및 로그아웃됩니다. (TOKEN_REUSE_DETECTED)
            """
    )
    @ApiErrorCodeExamples({
            "UNAUTHORIZED", "EXPIRED_TOKEN", "INVALID_TOKEN", "TOKEN_REUSE_DETECTED", "BLACKLISTED_TOKEN"
    })
    CommonResponse<SocialLoginResponse> reissue(HttpServletRequest request, HttpServletResponse response);

    @Operation(
            summary = "로그아웃",
            description = """
            현재 사용자의 세션을 종료하고 리프레시 토큰 쿠키를 제거합니다.
            """
    )
    @ApiErrorCodeExamples({
            "UNAUTHORIZED", "EXPIRED_TOKEN", "INVALID_TOKEN", "TOKEN_REUSE_DETECTED", "BLACKLISTED_TOKEN"
    })
    CommonResponse<Void> logout(
            @Parameter(hidden = true) @CurrentMemberId Long memberId, @Parameter(hidden = true) @CurrentAccessToken String accessToken,
            HttpServletResponse response);

    @Operation(
            summary = "애플 서버 알림 수신 (S2S)",
            description = """
            Apple 서버가 사용자 상태 변경 알림(계정 연동 해제 등)을 보낼 때 이를 수신하여 서버 데이터를 동기화합니다. <br><br>
            
            **[수신 케이스]** <br>
            * CONSENT_REVOKED: 사용자가 Apple 설정에서 앱 연동을 해제한 경우 애플 연동만 해제합니다. 마지막 남은 연동이었다면 탈퇴 처리합니다. <br>
            * ACCOUNT_DELETE: Apple 계정이 삭제된 경우 CONSENT_REVOKED와 동일하게 처리합니다. (다른 소셜이 연동되어 있으면 계정은 유지)
            * EMAIL_ENABLED: 사용자가 Apple 설정에서 이메일 공유를 활성화한 경우(숨기기 해제) 해당 플래그를 Y로 설정합니다.
            * EMAIL_DISABLED: 사용자가 Apple 설정에서 이메일 공유를 비활성화한 경우(숨기기 설정) 해당 플래그를 N으로 설정합니다.
            """
    )
    @ApiErrorCodeExamples({
            "INVALID_APPLE_ID_TOKEN", "APPLE_PUBLIC_KEY_NOT_FOUND"
    })
    CommonResponse<Void> receiveAppleNotification(@Valid @RequestBody AppleNotificationRequest request);

    @Operation(
            summary = "애플 연동 세션 발급",
            description = """
            애플 계정 추가 연동을 시작하기 위한 일회용 state를 발급합니다. <br><br>

            **[사용 방법]** <br>
            1. 이 API를 호출해 state를 발급받습니다. (유효 시간 5분, 1회만 사용 가능) <br>
            2. 애플 인증 URL의 state 파라미터에 발급받은 값을 그대로 넣어 웹뷰로 엽니다.
               redirect_uri는 로그인과 동일한 서버 주소를 사용합니다. <br>
            3. 인증이 끝나면 애플이 서버 콜백을 호출해 연동을 처리하고, redirectUrl로 결과와 함께 리다이렉트합니다. <br><br>

            **[참고 사항]** <br>
            * redirectUrl은 서버에 등록된 허용 origin이어야 합니다. (INVALID_REDIRECT_URL) <br>
            * 이미 애플을 연동한 회원은 세션 발급 단계에서 차단됩니다. (ALREADY_LINKED_PROVIDER)
            """
    )
    @ApiErrorCodeExamples({
            "ALREADY_LINKED_PROVIDER", "INVALID_REDIRECT_URL", "APPLE_LINK_SESSION_CREATION_FAILED", "UNAUTHORIZED"
    })
    CommonResponse<AppleLinkSessionResponse> createAppleLinkSession(
            @Parameter(hidden = true) @CurrentMemberId Long memberId, @Valid @RequestBody CreateAppleLinkSessionRequest request);

    @Operation(
            summary = "소셜 계정 연동 (애플 제외)",
            description = """
            로그인 중인 계정에 다른 소셜 플랫폼 계정을 추가로 연동합니다. <br><br>

            **[요청 값]** <br>
            * 카카오(웹 OAuth): authorizationCode <br>
            * 카카오(네이티브 SDK): accessToken <br><br>

            **[참고 사항]** <br>
            * 애플은 인가 코드가 서버로 직접 전달되므로 이 API로 연동할 수 없습니다. '애플 연동 세션 발급' API를 사용해야 합니다. (APPLE_LINK_REQUIRES_LINK_SESSION) <br>
            * 해당 소셜 계정이 이미 다른 회원에게 연동되어 있으면 연동이 차단됩니다. (SOCIAL_ACCOUNT_ALREADY_LINKED) <br>
            * 이미 같은 플랫폼을 연동한 경우 추가로 연동할 수 없습니다. (ALREADY_LINKED_PROVIDER)
            """
    )
    @ApiErrorCodeExamples({
            "SOCIAL_ACCOUNT_ALREADY_LINKED", "ALREADY_LINKED_PROVIDER", "MISSING_SOCIAL_CREDENTIAL",
            "INVALID_SOCIAL_CODE", "INVALID_SOCIAL_PROVIDER", "SOCIAL_AUTHENTICATION_FAILED", "APPLE_LINK_REQUIRES_LINK_SESSION"
    })
    CommonResponse<Void> linkSocialAccount(
            @Parameter(hidden = true) @CurrentMemberId Long memberId, @PathVariable SocialProvider provider,
            @Valid @RequestBody LinkSocialAccountRequest request);

    @Operation(
            summary = "소셜 계정 연동 해제",
            description = """
            연동된 소셜 계정을 해제합니다. 해제 후 해당 플랫폼으로는 로그인할 수 없습니다. <br><br>

            **[참고 사항]** <br>
            * 계정 탈퇴가 아니므로 프로필·활동 데이터는 삭제되지 않고 계정에 그대로 유지됩니다. <br>
            * 해제 후에도 동일 플랫폼으로 재연동할 수 있습니다. <br>
            * 연동된 소셜 계정이 1개뿐이면 계정에 접근할 수 없게 되므로 해제할 수 없습니다. (CANNOT_UNLINK_LAST_PROVIDER)
            """
    )
    @ApiErrorCodeExamples({
            "CANNOT_UNLINK_LAST_PROVIDER", "PROVIDER_NOT_LINKED", "KAKAO_REVOKE_FAILED", "APPLE_REVOKE_FAILED"
    })
    CommonResponse<Void> unlinkSocialAccount(
            @Parameter(hidden = true) @CurrentMemberId Long memberId, @PathVariable SocialProvider provider);
}
