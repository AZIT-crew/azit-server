package com.youthexpedition.azit.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
    private Cors cors;
    private List<String> permitAllPaths;
    private List<String> allowedRedirectOrigins; // 소셜 인증 완료 후 복귀를 허용할 프론트 origin 목록

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins;
        private List<String> allowedMethods;
        private List<String> allowedHeaders;
        private boolean allowCredentials;
    }
}
