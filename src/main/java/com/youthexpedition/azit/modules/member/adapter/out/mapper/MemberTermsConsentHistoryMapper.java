package com.youthexpedition.azit.modules.member.adapter.out.mapper;

import com.youthexpedition.azit.modules.member.adapter.out.persistence.entity.MemberTermsConsentHistoryEntity;
import com.youthexpedition.azit.modules.member.domain.model.MemberTermsConsentHistory;
import org.springframework.stereotype.Component;

@Component
public class MemberTermsConsentHistoryMapper {

    public MemberTermsConsentHistory toDomain(MemberTermsConsentHistoryEntity entity) {
        return MemberTermsConsentHistory.builder()
                .id(entity.getId())
                .memberId(entity.getMemberId())
                .termsVersionId(entity.getTermsVersionId())
                .isAgreed(entity.isAgreed())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public MemberTermsConsentHistoryEntity toEntity(MemberTermsConsentHistory domain) {
        return MemberTermsConsentHistoryEntity.builder()
                .memberId(domain.getMemberId())
                .termsVersionId(domain.getTermsVersionId())
                .isAgreed(domain.isAgreed())
                .build();
    }
}
