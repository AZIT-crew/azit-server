package com.youthexpedition.azit.modules.notification.adapter.in.web;

import com.youthexpedition.azit.infrastructure.common.annotation.CurrentMemberId;
import com.youthexpedition.azit.infrastructure.common.response.CommonResponse;
import com.youthexpedition.azit.infrastructure.common.response.code.CommonSuccessCode;
import com.youthexpedition.azit.modules.notification.adapter.in.web.docs.DeviceTokenControllerDocs;
import com.youthexpedition.azit.modules.notification.adapter.in.web.dto.DeleteDeviceTokenRequest;
import com.youthexpedition.azit.modules.notification.adapter.in.web.dto.RegisterDeviceTokenRequest;
import com.youthexpedition.azit.modules.notification.application.port.in.DeviceTokenUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members/me/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController implements DeviceTokenControllerDocs {

    private final DeviceTokenUseCase deviceTokenUseCase;

    @PostMapping
    public CommonResponse<Void> registerDeviceToken(@CurrentMemberId Long memberId,
                                                    @Valid @RequestBody RegisterDeviceTokenRequest request) {
        deviceTokenUseCase.register(memberId, request.toCommand());

        return CommonResponse.of(CommonSuccessCode.SUCCESS);
    }

    @DeleteMapping
    public CommonResponse<Void> deleteDeviceToken(@CurrentMemberId Long memberId,
                                                  @Valid @RequestBody DeleteDeviceTokenRequest request) {
        deviceTokenUseCase.delete(memberId, request.token());

        return CommonResponse.of(CommonSuccessCode.SUCCESS);
    }
}
