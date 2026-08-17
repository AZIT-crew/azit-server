package com.youthexpedition.azit.infrastructure.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StringFormatUtil 단위 테스트")
class StringFormatUtilTest {

    @Nested
    @DisplayName("옵션 값 조합")
    class FormatOptionValues {

        @Test
        @DisplayName("성공 - 구분자로 옵션들을 이어 붙인다")
        void formatOptionValues_success_joinsWithSeparator() {
            assertThat(StringFormatUtil.formatOptionValues(List.of("블랙", "L")))
                    .isEqualTo("블랙 · L");
        }

        @Test
        @DisplayName("성공 - 옵션이 하나면 구분자 없이 그대로 반환한다")
        void formatOptionValues_success_whenSingleOption() {
            assertThat(StringFormatUtil.formatOptionValues(List.of("블랙"))).isEqualTo("블랙");
        }

        @Test
        @DisplayName("성공 - 옵션이 없으면 빈 문자열을 반환한다")
        void formatOptionValues_returnsEmpty_whenNullOrEmpty() {
            assertThat(StringFormatUtil.formatOptionValues(null)).isEmpty();
            assertThat(StringFormatUtil.formatOptionValues(List.of())).isEmpty();
        }
    }

    @Nested
    @DisplayName("주문 번호 포맷")
    class BuildFullOrderNumber {

        @Test
        @DisplayName("성공 - 주문 번호 앞에 접두어를 붙인다")
        void buildFullOrderNumber_success_addsPrefix() {
            assertThat(StringFormatUtil.buildFullOrderNumber("20260817001")).isEqualTo("#20260817001");
        }

        @Test
        @DisplayName("성공 - 주문 번호가 없으면 null을 반환한다")
        void buildFullOrderNumber_returnsNull_whenNull() {
            assertThat(StringFormatUtil.buildFullOrderNumber(null)).isNull();
        }
    }
}
