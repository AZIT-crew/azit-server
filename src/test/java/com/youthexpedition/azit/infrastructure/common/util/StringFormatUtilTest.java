package com.youthexpedition.azit.infrastructure.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StringFormatUtil 단위 테스트")
class StringFormatUtilTest {

    @Nested
    @DisplayName("이메일 마스킹")
    class MaskEmail {

        @Test
        @DisplayName("성공 - 앞 2자만 남기고 마스킹한다")
        void maskEmail_success_keepsFirstTwoCharacters() {
            assertThat(StringFormatUtil.maskEmail("azit@kakao.com")).isEqualTo("az**@kakao.com");
        }

        @Test
        @DisplayName("성공 - 원본 길이가 드러나지 않도록 마스킹 길이는 고정된다")
        void maskEmail_success_masksWithFixedLength() {
            assertThat(StringFormatUtil.maskEmail("a@b.com")).isEqualTo("a**@b.com");
            assertThat(StringFormatUtil.maskEmail("verylongaddress@b.com")).isEqualTo("ve**@b.com");
        }

        @Test
        @DisplayName("성공 - 애플 프라이빗 릴레이 주소도 동일하게 마스킹된다")
        void maskEmail_success_whenApplePrivateRelay() {
            assertThat(StringFormatUtil.maskEmail("abc123def@privaterelay.appleid.com"))
                    .isEqualTo("ab**@privaterelay.appleid.com");
        }

        @Test
        @DisplayName("성공 - 이메일이 없으면 null을 반환한다")
        void maskEmail_returnsNull_whenBlank() {
            assertThat(StringFormatUtil.maskEmail(null)).isNull();
            assertThat(StringFormatUtil.maskEmail("")).isNull();
            assertThat(StringFormatUtil.maskEmail("   ")).isNull();
        }

        @Test
        @DisplayName("성공 - 이메일 형식이 아니면 전체를 가린다")
        void maskEmail_masksEntirely_whenNotEmailFormat() {
            assertThat(StringFormatUtil.maskEmail("notAnEmail")).isEqualTo("**");
        }

        @Test
        @DisplayName("성공 - 로컬 파트가 비어 있어도 도메인은 유지된다")
        void maskEmail_success_whenLocalPartEmpty() {
            assertThat(StringFormatUtil.maskEmail("@kakao.com")).isEqualTo("**@kakao.com");
        }
    }
}
