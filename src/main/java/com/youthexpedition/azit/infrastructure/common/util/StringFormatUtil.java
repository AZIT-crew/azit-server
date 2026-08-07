package com.youthexpedition.azit.infrastructure.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringFormatUtil {

    private static final String OPTION_SEPARATOR = " · ";
    private static final String ORDER_NUMBER_PREFIX = "#";
    private static final String EMAIL_MASK = "**";
    private static final char EMAIL_DELIMITER = '@';
    private static final int EMAIL_VISIBLE_PREFIX_LENGTH = 2;

    /**
     * 옵션 + · + 옵션 형식으로 조합
     */
    public static String formatOptionValues(List<String> optionValues) {
        if (optionValues == null || optionValues.isEmpty()) {
            return "";
        }
        return String.join(OPTION_SEPARATOR, optionValues);
    }

    /**
     * 주문 번호 앞에 접두어(#) 붙임
     */
    public static String buildFullOrderNumber(String orderNumber) {
        if (orderNumber == null) return null;
        return ORDER_NUMBER_PREFIX + orderNumber;
    }

    /**
     * 이메일 마스킹 (az**@kakao.com 형식)
     * 앞 2자만 노출하고 나머지는 길이와 무관하게 고정 길이로 가려 원본 길이가 드러나지 않도록 한다.
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) return null;

        int delimiterIndex = email.indexOf(EMAIL_DELIMITER);
        if (delimiterIndex < 0) return EMAIL_MASK; // 이메일 형식이 아니면 전체를 가림

        String localPart = email.substring(0, delimiterIndex);
        String domainPart = email.substring(delimiterIndex);
        int visibleLength = Math.min(EMAIL_VISIBLE_PREFIX_LENGTH, localPart.length());

        return localPart.substring(0, visibleLength) + EMAIL_MASK + domainPart;
    }
}
