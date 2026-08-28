package com.youthexpedition.azit.modules.notification.application.port.in;

public interface RecordNotificationUseCase {
    void recordCrewJoinRequested(Long crewId, Long requesterMemberId);
    void recordCrewJoinApproved(Long crewId, Long targetMemberId);
    void recordCrewJoinRejected(Long crewId, Long targetMemberId);
}
