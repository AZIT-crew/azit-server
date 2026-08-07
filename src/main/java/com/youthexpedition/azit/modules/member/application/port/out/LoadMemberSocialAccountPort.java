package com.youthexpedition.azit.modules.member.application.port.out;

import com.youthexpedition.azit.modules.member.domain.model.MemberSocialAccount;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;

import java.util.List;
import java.util.Optional;

public interface LoadMemberSocialAccountPort {
    Optional<MemberSocialAccount> findBySocialInfo(SocialProvider socialProvider, String socialProviderId);
    List<MemberSocialAccount> findAllByMemberId(Long memberId);
}
