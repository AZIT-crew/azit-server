package com.youthexpedition.azit.modules.notification.adapter.in.web.docs;

import com.youthexpedition.azit.infrastructure.common.annotation.CurrentMemberId;
import com.youthexpedition.azit.infrastructure.common.response.CommonResponse;
import com.youthexpedition.azit.infrastructure.config.swagger.ApiErrorCodeExamples;
import com.youthexpedition.azit.modules.notification.adapter.in.web.dto.DeleteDeviceTokenRequest;
import com.youthexpedition.azit.modules.notification.adapter.in.web.dto.RegisterDeviceTokenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Notification", description = "알림 API")
public interface DeviceTokenControllerDocs {

    @Operation(
            summary = "기기 토큰 등록",
            description = """
            푸시 알림을 받을 FCM 기기 토큰을 등록합니다. <br><br>

            **[호출 시점]** <br>
            * 로그인 직후, 그리고 FCM 토큰이 갱신될 때마다 호출하세요. <br>
            * 같은 토큰으로 다시 호출해도 됩니다(덮어쓰기). <br><br>

            **[참고 사항]** <br>
            * 한 회원이 여러 기기를 등록할 수 있고, 등록된 모든 기기로 푸시가 발송됩니다. <br>
            * 같은 기기를 다른 계정이 등록하면 소유자가 새 계정으로 옮겨집니다. 이전 계정으로는 더 이상 알림이 가지 않습니다. <br>
            * 앱 삭제·재설치로 무효해진 토큰은 발송 시점에 서버가 정리하므로 별도 처리가 필요 없습니다.
            """
    )
    @ApiErrorCodeExamples({
            "INVALID_INPUT_VALUE",
            "UNAUTHORIZED", "EXPIRED_TOKEN", "INVALID_TOKEN", "TOKEN_REUSE_DETECTED", "BLACKLISTED_TOKEN"
    })
    CommonResponse<Void> registerDeviceToken(@Parameter(hidden = true) @CurrentMemberId Long memberId,
                                             @Valid @RequestBody RegisterDeviceTokenRequest request);

    @Operation(
            summary = "기기 토큰 삭제",
            description = """
            로그아웃 시 해당 기기로 더 이상 푸시가 가지 않도록 토큰을 삭제합니다. <br><br>

            **[참고 사항]** <br>
            * 등록되지 않은 토큰이거나 다른 회원의 토큰일 경우 오류 처리 하지 않고 성공을 반환합니다(로그아웃 흐름을 막지 않기 위함).
            """
    )
    @ApiErrorCodeExamples({
            "INVALID_INPUT_VALUE",
            "UNAUTHORIZED", "EXPIRED_TOKEN", "INVALID_TOKEN", "TOKEN_REUSE_DETECTED", "BLACKLISTED_TOKEN"
    })
    CommonResponse<Void> deleteDeviceToken(@Parameter(hidden = true) @CurrentMemberId Long memberId,
                                           @Valid @RequestBody DeleteDeviceTokenRequest request);
}
