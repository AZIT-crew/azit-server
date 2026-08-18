package com.youthexpedition.azit.modules.member.adapter.in.web.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UpdateOptionalTermsRequest 검증 단위 테스트")
class UpdateOptionalTermsRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("성공: 한 항목만 담아 보내는 부분 갱신 요청은 통과한다.")
    void validate_success_whenOnlyOneTermSpecified() {
        // given
        UpdateOptionalTermsRequest request = new UpdateOptionalTermsRequest(false, null);

        // when & then
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("실패: 두 항목이 모두 null이면 변경할 대상이 없어 검증에 실패한다.")
    void validate_fail_whenNoTermSpecified() {
        // given
        UpdateOptionalTermsRequest request = new UpdateOptionalTermsRequest(null, null);

        // when & then
        assertThat(validator.validate(request))
                .singleElement()
                .satisfies(violation -> {
                    assertThat(violation.getMessage()).isEqualTo("변경할 선택 약관 항목이 없습니다.");
                    assertThat(violation.getPropertyPath()).hasToString("anyTermsSpecified"); // 필드 에러로 매핑되어야 핸들러가 메시지를 읽는다
                });
    }
}
