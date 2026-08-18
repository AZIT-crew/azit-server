package com.youthexpedition.azit.modules.auth.application.port.out;

import com.youthexpedition.azit.modules.auth.domain.model.AppleLinkSession;

import java.util.Optional;

public interface AppleLinkSessionPort {
    void save(String state, AppleLinkSession session, long durationSeconds);

    /**
     * state에 해당하는 세션을 조회하고 즉시 삭제(일회용).
     */
    Optional<AppleLinkSession> consume(String state);
}
