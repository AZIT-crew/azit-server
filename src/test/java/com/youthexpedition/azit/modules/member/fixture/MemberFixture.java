package com.youthexpedition.azit.modules.member.fixture;

import com.youthexpedition.azit.modules.member.domain.model.Member;
import com.youthexpedition.azit.modules.member.domain.model.enums.MemberRole;
import com.youthexpedition.azit.modules.member.domain.model.enums.MemberStatus;

/**
 * 회원 테스트에서 반복 사용되는 Member 픽스처.
 * Member.create()는 가입 직전 상태라 id가 없으므로, 조회된 회원이 필요한 경우 빌더로 구성한다.
 */
public class MemberFixture {

    public static final String NICKNAME = "nickname";
    public static final String EMAIL = "test@example.com";
    public static final String PROFILE_IMAGE_URL = "imageUrl";

    public static Member activeMember(Long id) {
        return member(id, MemberStatus.ACTIVE);
    }

    public static Member member(Long id, MemberStatus status) {
        return Member.builder()
                .id(id)
                .nickname(NICKNAME)
                .status(status)
                .role(MemberRole.MEMBER)
                .totalPoints(0L)
                .totalAttendanceCount(0)
                .build();
    }

    // 가입 직전 상태(id 없음)의 회원
    public static Member newMember() {
        return Member.create(NICKNAME, EMAIL, PROFILE_IMAGE_URL);
    }
}
