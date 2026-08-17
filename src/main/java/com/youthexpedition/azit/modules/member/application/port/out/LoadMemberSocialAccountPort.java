package com.youthexpedition.azit.modules.member.application.port.out;

import com.youthexpedition.azit.modules.member.domain.model.MemberSocialAccount;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;

import java.util.List;
import java.util.Optional;

public interface LoadMemberSocialAccountPort {
    Optional<MemberSocialAccount> findBySocialInfo(SocialProvider socialProvider, String socialProviderId);
    List<MemberSocialAccount> findAllByMemberId(Long memberId);

    // 연동 해제처럼 조회 결과로 판정한 뒤 삭제까지 원자적으로 처리해야 하는 경우에 사용
    List<MemberSocialAccount> findAllByMemberIdForUpdate(Long memberId);
}
