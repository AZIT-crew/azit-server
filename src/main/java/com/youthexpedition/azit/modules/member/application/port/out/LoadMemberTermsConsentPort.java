package com.youthexpedition.azit.modules.member.application.port.out;

import com.youthexpedition.azit.modules.member.domain.model.MemberTermsConsentHistory;
import com.youthexpedition.azit.modules.member.domain.model.enums.TermsType;

import java.util.Optional;

public interface LoadMemberTermsConsentPort {
    // 특정 약관의 가장 최근 동의/거부 이력 (동의 상태를 변경한 시점 조회용)
    Optional<MemberTermsConsentHistory> findLatestHistory(Long memberId, TermsType termsType);
}
