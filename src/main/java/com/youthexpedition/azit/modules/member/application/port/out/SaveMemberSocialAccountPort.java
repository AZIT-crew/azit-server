package com.youthexpedition.azit.modules.member.application.port.out;

import com.youthexpedition.azit.modules.member.domain.model.MemberSocialAccount;

public interface SaveMemberSocialAccountPort {
    MemberSocialAccount save(MemberSocialAccount socialAccount);
    void deleteByMemberId(Long memberId);
}
