package com.youthexpedition.azit.modules.member.adapter.out.mapper;

import com.youthexpedition.azit.modules.member.adapter.out.persistence.entity.MemberSocialAccountEntity;
import com.youthexpedition.azit.modules.member.domain.model.MemberSocialAccount;
import org.springframework.stereotype.Component;

@Component
public class MemberSocialAccountMapper {
    public MemberSocialAccount toDomain(MemberSocialAccountEntity entity) {
        if (entity == null) return null;

        return MemberSocialAccount.builder()
                .id(entity.getId())
                .memberId(entity.getMemberId())
                .socialProvider(entity.getSocialProvider())
                .socialProviderId(entity.getSocialProviderId())
                .email(entity.getEmail())
                .isEmailSharingEnabled(entity.isEmailSharingEnabled())
                .appleRefreshToken(entity.getAppleRefreshToken())
                .linkedAt(entity.getLinkedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public MemberSocialAccountEntity toEntity(MemberSocialAccount domain) {
        return MemberSocialAccountEntity.builder()
                .id(domain.getId())
                .memberId(domain.getMemberId())
                .socialProvider(domain.getSocialProvider())
                .socialProviderId(domain.getSocialProviderId())
                .email(domain.getEmail())
                .isEmailSharingEnabled(domain.isEmailSharingEnabled())
                .appleRefreshToken(domain.getAppleRefreshToken())
                .linkedAt(domain.getLinkedAt())
                .build();
    }
}
