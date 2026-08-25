package com.youthexpedition.azit.modules.member.application.port.in.command;

public record UpdateCrewNotificationSettingCommand(
        Boolean allEnabled,            // 크루 전체알림. 지정 시 정기런·번개런을 한 번에 변경
        Boolean regularRunEnabled,     // 정기런 알림
        Boolean lightningRunEnabled    // 번개런 알림
) {
    public static UpdateCrewNotificationSettingCommand of(Boolean allEnabled, Boolean regularRunEnabled,
                                                          Boolean lightningRunEnabled) {
        return new UpdateCrewNotificationSettingCommand(allEnabled, regularRunEnabled, lightningRunEnabled);
    }
}
