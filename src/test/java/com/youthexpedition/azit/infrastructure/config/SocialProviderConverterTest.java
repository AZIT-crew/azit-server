package com.youthexpedition.azit.infrastructure.config;

import com.youthexpedition.azit.infrastructure.common.response.CommonResponse;
import com.youthexpedition.azit.infrastructure.common.response.code.CommonSuccessCode;
import com.youthexpedition.azit.infrastructure.exception.BusinessException;
import com.youthexpedition.azit.infrastructure.exception.GlobalExceptionHandler;
import com.youthexpedition.azit.modules.auth.domain.model.enums.AuthErrorCode;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("SocialProviderConverter 단위 테스트")
class SocialProviderConverterTest {

    private final SocialProviderConverter socialProviderConverter = new SocialProviderConverter();

    @Test
    @DisplayName("성공 - 소문자로 들어와도 Enum으로 변환된다")
    void convert_success_whenLowerCase() {
        assertThat(socialProviderConverter.convert("kakao")).isEqualTo(SocialProvider.KAKAO);
        assertThat(socialProviderConverter.convert("APPLE")).isEqualTo(SocialProvider.APPLE);
    }

    @Test
    @DisplayName("실패 - 지원하지 않는 플랫폼이면 INVALID_SOCIAL_PROVIDER 예외가 발생한다")
    void convert_throwsException_whenUnsupportedProvider() {
        assertThatThrownBy(() -> socialProviderConverter.convert("naver"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_SOCIAL_PROVIDER);
    }

    @Nested
    @DisplayName("웹 요청 처리")
    class WebRequest {

        @RestController
        static class TestController {
            @PostMapping("/test/{provider}")
            CommonResponse<Void> handle(@PathVariable SocialProvider provider) {
                return CommonResponse.of(CommonSuccessCode.SUCCESS);
            }
        }

        private MockMvc mockMvc() {
            FormattingConversionService conversionService = new FormattingConversionService();
            conversionService.addConverter(new SocialProviderConverter());

            return MockMvcBuilders.standaloneSetup(new TestController())
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .setConversionService(conversionService)
                    .build();
        }

        @Test
        @DisplayName("실패 - 지원하지 않는 플랫폼은 TYPE_MISMATCH_ERROR가 아닌 INVALID_SOCIAL_PROVIDER로 응답한다")
        void handle_respondsWithInvalidSocialProvider_whenUnsupportedProvider() throws Exception {
            mockMvc().perform(post("/test/naver"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_SOCIAL_PROVIDER"));
        }

        @Test
        @DisplayName("성공 - 지원하는 플랫폼은 정상 처리된다")
        void handle_success_whenSupportedProvider() throws Exception {
            mockMvc().perform(post("/test/kakao"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }
}
