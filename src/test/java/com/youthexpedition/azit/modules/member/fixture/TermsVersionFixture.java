package com.youthexpedition.azit.modules.member.fixture;

import com.youthexpedition.azit.modules.member.domain.model.TermsVersion;
import com.youthexpedition.azit.modules.member.domain.model.enums.TermsType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 약관 테스트에서 반복 사용되는 TermsVersion 픽스처.
 * 종류별 최신 버전 1건씩을 조회하는 LoadTermsVersionPort#findAllLatest 스텁 용도로 사용한다.
 */
public class TermsVersionFixture {

    public static final Long SERVICE_VERSION_ID = 1L;
    public static final Long PRIVACY_VERSION_ID = 2L;
    public static final Long LOCATION_VERSION_ID = 3L;
    public static final Long THIRD_PARTY_VERSION_ID = 4L;
    public static final Long MARKETING_VERSION_ID = 5L;
    public static final Long NOTIFICATION_VERSION_ID = 6L;

    private static final String VERSION = "1.0";
    private static final LocalDateTime EFFECTIVE_AT = LocalDateTime.of(2024, 1, 1, 0, 0);

    // 필수 4종 + 선택 2종 전체
    public static List<TermsVersion> allLatest() {
        return List.of(
                termsVersion(SERVICE_VERSION_ID, TermsType.SERVICE, true),
                termsVersion(PRIVACY_VERSION_ID, TermsType.PRIVACY, true),
                termsVersion(LOCATION_VERSION_ID, TermsType.LOCATION, true),
                termsVersion(THIRD_PARTY_VERSION_ID, TermsType.THIRD_PARTY, true),
                termsVersion(MARKETING_VERSION_ID, TermsType.MARKETING, false),
                termsVersion(NOTIFICATION_VERSION_ID, TermsType.NOTIFICATION, false)
        );
    }

    public static TermsVersion termsVersion(Long id, TermsType termsType, boolean isRequired) {
        return TermsVersion.builder()
                .id(id)
                .termsType(termsType)
                .version(VERSION)
                .isRequired(isRequired)
                .effectiveAt(EFFECTIVE_AT)
                .createdAt(EFFECTIVE_AT)
                .build();
    }
}
