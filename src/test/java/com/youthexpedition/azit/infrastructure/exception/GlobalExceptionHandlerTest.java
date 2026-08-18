package com.youthexpedition.azit.infrastructure.exception;

import com.youthexpedition.azit.infrastructure.common.response.CommonErrorResponse;
import com.youthexpedition.azit.infrastructure.common.response.code.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler 단위 테스트")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Nested
    @DisplayName("DTO 검증 실패 처리")
    class HandleMethodArgumentNotValid {

        @Test
        @DisplayName("성공 - 필드 단위 제약 위반은 해당 필드의 메시지를 반환한다")
        void handleMethodArgumentNotValid_returnsFieldErrorMessage() {
            // given
            BindingResult bindingResult = bindingResult();
            bindingResult.rejectValue("nickname", "NotBlank", "닉네임은 필수입니다.");

            // when
            ResponseEntity<CommonErrorResponse> response =
                    globalExceptionHandler.handleMethodArgumentNotValidException(methodArgumentNotValidException(bindingResult));

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().code()).isEqualTo(CommonErrorCode.INVALID_INPUT_VALUE.getCode());
            assertThat(response.getBody().message()).isEqualTo("닉네임은 필수입니다.");
        }

        @Test
        @DisplayName("성공 - 클래스 단위 제약 위반(FieldError 없음)도 메시지를 반환한다")
        void handleMethodArgumentNotValid_returnsGlobalErrorMessage() {
            // given - 클래스 레벨 제약은 필드가 없는 ObjectError로만 잡힌다
            BindingResult bindingResult = bindingResult();
            bindingResult.reject("ValidOptionalTerms", "변경할 선택 약관 항목이 없습니다.");

            // when
            ResponseEntity<CommonErrorResponse> response =
                    globalExceptionHandler.handleMethodArgumentNotValidException(methodArgumentNotValidException(bindingResult));

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().message()).isEqualTo("변경할 선택 약관 항목이 없습니다.");
        }

        @Test
        @DisplayName("성공 - 메시지가 없는 위반이면 기본 문구를 반환한다")
        void handleMethodArgumentNotValid_returnsDefaultMessage_whenNoErrorMessage() {
            // given
            BindingResult bindingResult = bindingResult();
            bindingResult.reject("Invalid"); // defaultMessage 없음

            // when
            ResponseEntity<CommonErrorResponse> response =
                    globalExceptionHandler.handleMethodArgumentNotValidException(methodArgumentNotValidException(bindingResult));

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().message()).isEqualTo(CommonErrorCode.INVALID_INPUT_VALUE.getMessage());
        }

        private BindingResult bindingResult() {
            return new BeanPropertyBindingResult(new TestRequest("nickname"), "testRequest");
        }

        private MethodArgumentNotValidException methodArgumentNotValidException(BindingResult bindingResult) {
            try {
                Method method = TestController.class.getDeclaredMethod("handle", TestRequest.class);
                return new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private record TestRequest(String nickname) {
    }

    private static class TestController {
        @SuppressWarnings("unused")
        void handle(TestRequest request) {
        }
    }
}
