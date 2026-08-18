package com.youthexpedition.azit.infrastructure.auth.util;

import com.youthexpedition.azit.infrastructure.config.SecurityProperties;
import com.youthexpedition.azit.infrastructure.exception.BusinessException;
import com.youthexpedition.azit.modules.auth.domain.model.enums.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedirectUrlValidator {

    private final SecurityProperties securityProperties;

    public void validate(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank()) {
            throw new BusinessException(AuthErrorCode.INVALID_REDIRECT_URL);
        }

        if (!isAllowed(redirectUrl)) {
            log.warn("허용되지 않은 리다이렉트 주소 요청입니다: {}", redirectUrl);
            throw new BusinessException(AuthErrorCode.INVALID_REDIRECT_URL);
        }
    }

    public boolean isAllowed(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank()) {
            return false;
        }

        List<String> allowedOrigins = securityProperties.getAllowedRedirectOrigins();
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            return false;
        }

        String origin = extractOrigin(redirectUrl);
        return origin != null && allowedOrigins.contains(origin);
    }

    private String extractOrigin(String redirectUrl) {
        try {
            URI uri = new URI(redirectUrl);
            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (scheme == null || host == null) {
                return null;
            }

            String origin = scheme.toLowerCase() + "://" + host.toLowerCase(); // 주소에서 scheme://host[:port] 형태의 origin만 추출
            return uri.getPort() == -1 ? origin : origin + ":" + uri.getPort();
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
