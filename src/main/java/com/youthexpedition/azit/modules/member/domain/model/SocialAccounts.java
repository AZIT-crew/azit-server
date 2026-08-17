package com.youthexpedition.azit.modules.member.domain.model;

import com.youthexpedition.azit.infrastructure.exception.BusinessException;
import com.youthexpedition.azit.modules.member.domain.model.enums.MemberErrorCode;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class SocialAccounts {
    private final List<MemberSocialAccount> accounts;

    public static SocialAccounts of(List<MemberSocialAccount> accounts) {
        return new SocialAccounts(accounts == null ? List.of() : List.copyOf(accounts));
    }

    public boolean hasProvider(SocialProvider socialProvider) {
        return accounts.stream().anyMatch(account -> account.getSocialProvider() == socialProvider);
    }

    // 연동 해제가 가능한 상태인지 (해제 버튼 활성화 여부 판단에 사용)
    public boolean isUnlinkable() {
        return accounts.size() > 1;
    }

    /**
     * 해제 대상 소셜 계정을 반환한다. 실제 삭제와 플랫폼 연동 해제는 호출자가 수행한다.
     */
    public MemberSocialAccount unlink(SocialProvider socialProvider) {
        MemberSocialAccount target = accounts.stream()
                .filter(account -> account.getSocialProvider() == socialProvider)
                .findFirst()
                .orElseThrow(() -> new BusinessException(MemberErrorCode.PROVIDER_NOT_LINKED));

        if (!isUnlinkable()) {
            throw new BusinessException(MemberErrorCode.CANNOT_UNLINK_LAST_PROVIDER);
        }

        return target;
    }
}
