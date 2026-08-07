package com.youthexpedition.azit.modules.member.adapter.out.mapper;

import com.youthexpedition.azit.modules.member.adapter.out.persistence.entity.MemberEntity;
import com.youthexpedition.azit.modules.member.domain.model.Member;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {
    public Member toDomain(MemberEntity entity) {
        if (entity == null) return null;

        return Member.builder()
                .id(entity.getId())
                .nickname(entity.getNickname())
                .email(entity.getEmail())
                .profileImageUrl(entity.getProfileImageUrl())
                .status(entity.getStatus())
                .role(entity.getRole())
                .totalPoints(entity.getTotalPoints())
                .totalAttendanceCount(entity.getTotalAttendanceCount())
                .essentialTermsAgreedAt(entity.getEssentialTermsAgreedAt())
                .isMarketingTermsAgreed(entity.isMarketingTermsAgreed())
                .marketingTermsAgreedAt(entity.getMarketingTermsAgreedAt())
                .isNotificationAgreed(entity.isNotificationAgreed())
                .notificationAgreedAt(entity.getNotificationAgreedAt())
                .withdrawnAt(entity.getWithdrawnAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public MemberEntity toEntity(Member domain) {
        return MemberEntity.builder()
                .id(domain.getId())
                .nickname(domain.getNickname())
                .email(domain.getEmail())
                .profileImageUrl(domain.getProfileImageUrl())
                .status(domain.getStatus())
                .role(domain.getRole())
                .totalPoints(domain.getTotalPoints())
                .totalAttendanceCount(domain.getTotalAttendanceCount())
                .essentialTermsAgreedAt(domain.getEssentialTermsAgreedAt())
                .isMarketingTermsAgreed(domain.isMarketingTermsAgreed())
                .marketingTermsAgreedAt(domain.getMarketingTermsAgreedAt())
                .isNotificationAgreed(domain.isNotificationAgreed())
                .notificationAgreedAt(domain.getNotificationAgreedAt())
                .withdrawnAt(domain.getWithdrawnAt())
                .build();
    }
}
