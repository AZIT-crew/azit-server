package com.youthexpedition.azit.modules.member.domain.model;

import com.youthexpedition.azit.infrastructure.exception.BusinessException;
import com.youthexpedition.azit.modules.member.domain.model.enums.MemberErrorCode;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;

import java.util.List;

/**
 * 한 회원에 연동된 소셜 계정 묶음.
 *
 * "최소 1개는 남아 있어야 한다"는 규칙은 계정 하나만으로는 판단할 수 없어 이 컬렉션이 책임진다.
 * 마지막 연동까지 해제되면 로그인 수단이 사라져 계정에 접근할 수 없게 되므로(계정 미아) 차단한다.
 */
public class SocialAccounts {
    private final List<MemberSocialAccount> accounts;

    private SocialAccounts(List<MemberSocialAccount> accounts) {
        this.accounts = accounts;
    }

    public static SocialAccounts of(List<MemberSocialAccount> accounts) {
        return new SocialAccounts(accounts == null ? List.of() : List.copyOf(accounts));
    }

    public List<MemberSocialAccount> getAccounts() {
        return accounts;
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
