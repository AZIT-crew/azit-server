package com.youthexpedition.azit.modules.auth.application.port.in.command;

public record CreateAppleLinkSessionCommand(
        Long memberId,
        String redirectUrl // 연동 완료 후 돌아갈 프론트 주소
) {
    public static CreateAppleLinkSessionCommand of(Long memberId, String redirectUrl) {
        return new CreateAppleLinkSessionCommand(memberId, redirectUrl);
    }
}
