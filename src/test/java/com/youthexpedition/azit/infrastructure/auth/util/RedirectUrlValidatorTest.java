package com.youthexpedition.azit.infrastructure.auth.util;

import com.youthexpedition.azit.infrastructure.config.SecurityProperties;
import com.youthexpedition.azit.infrastructure.exception.BusinessException;
import com.youthexpedition.azit.modules.auth.domain.model.enums.AuthErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RedirectUrlValidator 단위 테스트")
class RedirectUrlValidatorTest {

    private RedirectUrlValidator redirectUrlValidator;

    @BeforeEach
    void setUp() {
        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.setAllowedRedirectOrigins(List.of("https://azitcrew.com", "http://localhost:5173"));
        redirectUrlValidator = new RedirectUrlValidator(securityProperties);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://azitcrew.com",
            "https://azitcrew.com/settings/accounts",
            "https://AZITCREW.com/settings/accounts",
            "https://azitcrew.com/settings?tab=social",
            "http://localhost:5173/auth/callback"
    })
    @DisplayName("성공 - 허용 목록의 origin이면 경로·쿼리와 무관하게 통과한다")
    void isAllowed_returnsTrue_whenOriginAllowed(String redirectUrl) {
        assertThat(redirectUrlValidator.isAllowed(redirectUrl)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://evil.com/callback",
            "https://azitcrew.com.evil.com/callback", // 접두사만 같은 다른 도메인
            "http://azitcrew.com/callback",           // scheme 불일치
            "https://azitcrew.com:8443/callback",     // port 불일치
            "/settings/accounts",                     // 상대 경로
            "javascript:alert(1)",
            "not a url"
    })
    @DisplayName("실패 - 허용 목록에 없는 주소는 차단한다")
    void isAllowed_returnsFalse_whenOriginNotAllowed(String redirectUrl) {
        assertThat(redirectUrlValidator.isAllowed(redirectUrl)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("실패 - 주소가 비어 있으면 차단한다")
    void validate_throwsException_whenRedirectUrlBlank(String redirectUrl) {
        assertThatThrownBy(() -> redirectUrlValidator.validate(redirectUrl))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_REDIRECT_URL);
    }

    @Test
    @DisplayName("실패 - 허용 목록이 비어 있으면 어떤 주소도 통과시키지 않는다")
    void isAllowed_returnsFalse_whenNoAllowedOriginsConfigured() {
        // given - 설정 누락 시 모든 주소를 허용하는 사고를 막는다
        SecurityProperties emptyProperties = new SecurityProperties();
        RedirectUrlValidator validator = new RedirectUrlValidator(emptyProperties);

        // when & then
        assertThat(validator.isAllowed("https://azitcrew.com/settings")).isFalse();
    }

    @Test
    @DisplayName("실패 - 허용되지 않은 주소로 validate하면 예외가 발생한다")
    void validate_throwsException_whenOriginNotAllowed() {
        assertThatThrownBy(() -> redirectUrlValidator.validate("https://evil.com"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_REDIRECT_URL);
    }
}
