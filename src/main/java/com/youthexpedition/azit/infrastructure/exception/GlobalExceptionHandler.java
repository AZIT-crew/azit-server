package com.youthexpedition.azit.infrastructure.exception;

import com.youthexpedition.azit.infrastructure.common.response.CommonErrorResponse;
import com.youthexpedition.azit.infrastructure.common.response.code.CommonErrorCode;
import com.youthexpedition.azit.infrastructure.common.response.code.BaseErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 중 발생하는 커스텀 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<CommonErrorResponse> handleBusinessException(BusinessException e) {
        BaseErrorCode errorCode = e.getErrorCode();

        // 서버 관련 에러일 때만 error 로깅
        if (errorCode.getStatus().is5xxServerError()) {
            log.error("BusinessException (Server Error): {}", errorCode.getMessage(), e);
        } else {
            log.warn("BusinessException (Client Error): {}", errorCode.getMessage());
        }

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(CommonErrorResponse.of(errorCode));
    }

    /**
     * 400 Error: @Valid 어노테이션으로 DTO 검증 실패 시 발생
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<CommonErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("MethodArgumentNotValidException: {}", e.getMessage());

        String errorMessage = resolveValidationMessage(e.getBindingResult());

        return ResponseEntity
                .status(CommonErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(CommonErrorResponse.of(
                        CommonErrorCode.INVALID_INPUT_VALUE.getCode(),
                        errorMessage
                ));
    }

    /**
     * 검증 실패 메시지 추출.
     * 필드 단위 제약(@NotNull 등)은 FieldError로, 클래스 단위 제약은 필드 없는 ObjectError로 잡히므로 둘 다 처리
     */
    private String resolveValidationMessage(BindingResult bindingResult) {
        FieldError fieldError = bindingResult.getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null) {
            return fieldError.getDefaultMessage(); // 어떤 필드가 잘못됐는지 알 수 있는 메시지 우선
        }

        return bindingResult.getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(CommonErrorCode.INVALID_INPUT_VALUE.getMessage()); // 메시지가 없으면 기본 문구
    }

    /**
     * 400 Error: 메서드 파라미터 타입이 일치하지 않을 때 발생 (예: 숫자가 들어와야 하는데 문자가 들어온 경우)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<CommonErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        // 커스텀 컨버터가 던진 BusinessException은 변환 실패로 감싸이므로 원래 에러 코드로 응답
        if (NestedExceptionUtils.getMostSpecificCause(e) instanceof BusinessException businessException) {
            return handleBusinessException(businessException);
        }

        log.warn("MethodArgumentTypeMismatchException: {}", e.getMessage());

        return ResponseEntity
                .status(CommonErrorCode.TYPE_MISMATCH_ERROR.getStatus())
                .body(CommonErrorResponse.of(CommonErrorCode.TYPE_MISMATCH_ERROR));
    }

    /**
     * 405 Error: 지원하지 않는 HTTP 메서드 호출 시 발생 (예: POST인데 GET으로 보낸 경우)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<CommonErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("HttpRequestMethodNotSupportedException: {}", e.getMessage());

        return ResponseEntity
                .status(CommonErrorCode.METHOD_NOT_ALLOWED.getStatus())
                .body(CommonErrorResponse.of(CommonErrorCode.METHOD_NOT_ALLOWED));
    }

    /**
     * 500 Error: 그 외 정의되지 않은 모든 서버 내부 에러 처리
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<CommonErrorResponse> handleException(Exception e) {
        log.error("Unhandled Exception: ", e);

        return ResponseEntity
                .status(CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(CommonErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR));
    }
}