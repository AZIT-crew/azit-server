package com.youthexpedition.azit.modules.member.application.service.mapper;

import com.youthexpedition.azit.infrastructure.common.util.image.ImageUrlFormatUtil;
import com.youthexpedition.azit.modules.crew.domain.model.Crew;
import com.youthexpedition.azit.modules.crew.domain.model.CrewMember;
import com.youthexpedition.azit.modules.crew.domain.model.enums.CrewMemberStatus;
import com.youthexpedition.azit.modules.member.application.port.in.dto.LinkedProviderResponse;
import com.youthexpedition.azit.modules.member.application.port.in.dto.LinkedProviderResponse.LinkedProviderItem;
import com.youthexpedition.azit.modules.member.application.port.in.dto.MyCrewResponse;
import com.youthexpedition.azit.modules.member.application.port.in.dto.MyInfoResponse;
import com.youthexpedition.azit.modules.member.domain.model.Member;
import com.youthexpedition.azit.modules.member.domain.model.MemberSocialAccount;
import com.youthexpedition.azit.modules.member.domain.model.SocialAccounts;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MemberResponseMapper {

    private final ImageUrlFormatUtil imageUrlFormatUtil;

    public MyInfoResponse toMyInfoResponse(Member member) {
        return MyInfoResponse.of(
                member.getId(),
                member.getNickname(),
                imageUrlFormatUtil.buildFullImageUrl(member.getProfileImageUrl()),
                member.getTotalPoints()
        );
    }

    public LinkedProviderResponse toLinkedProviderResponse(SocialAccounts socialAccounts) {
        Map<SocialProvider, MemberSocialAccount> accountsByProvider = socialAccounts.getAccounts().stream()
                .collect(Collectors.toMap(
                        MemberSocialAccount::getSocialProvider,
                        account -> account,
                        (existing, duplicate) -> existing // 플랫폼당 1개
                ));

        List<LinkedProviderItem> providers = Arrays.stream(SocialProvider.values())
                .map(provider -> toLinkedProviderItem(
                        provider, accountsByProvider.get(provider), socialAccounts.isUnlinkable()))
                .toList();

        return LinkedProviderResponse.of(providers);
    }

    private LinkedProviderItem toLinkedProviderItem(SocialProvider provider, MemberSocialAccount socialAccount,
                                                    boolean isUnlinkable) {
        if (socialAccount == null) {
            return LinkedProviderItem.notLinked(provider);
        }

        LocalDate linkedAt = socialAccount.getLinkedAt() == null ? null : socialAccount.getLinkedAt().toLocalDate();
        return LinkedProviderItem.linked(
                provider,
                socialAccount.getEmail(),
                linkedAt,
                isUnlinkable
        );
    }

    public MyCrewResponse toMyCrewResponse(CrewMember crewMember, Crew crew) {
        // JOINED 상태일 때 초대 코드 노출
        String invitationCode = crewMember.getStatus() == CrewMemberStatus.JOINED
                ? crew.getInvitationCode() : null;

        return MyCrewResponse.of(
                crew.getId(),
                crew.getName(),
                imageUrlFormatUtil.buildFullImageUrl(crew.getImageUrl()),
                crewMember.getStatus() == CrewMemberStatus.REQUESTED ? null : crewMember.getRole(),
                crewMember.getStatus(),
                invitationCode
        );
    }
}
