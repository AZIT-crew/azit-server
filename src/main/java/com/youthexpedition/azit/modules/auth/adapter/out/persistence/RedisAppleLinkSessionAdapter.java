package com.youthexpedition.azit.modules.auth.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youthexpedition.azit.infrastructure.exception.BusinessException;
import com.youthexpedition.azit.modules.auth.application.port.out.AppleLinkSessionPort;
import com.youthexpedition.azit.modules.auth.domain.model.AppleLinkSession;
import com.youthexpedition.azit.modules.auth.domain.model.enums.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAppleLinkSessionAdapter implements AppleLinkSessionPort {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String APPLE_LINK_PREFIX = "APPLE_LINK:";

    private String linkKey(String state) { return APPLE_LINK_PREFIX + state; }

    @Override
    public void save(String state, AppleLinkSession session, long durationSeconds) {
        try {
            redisTemplate.opsForValue()
                    .set(linkKey(state), objectMapper.writeValueAsString(session), durationSeconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("애플 연동 세션 직렬화에 실패했습니다: {}", e.getMessage());
            throw new BusinessException(AuthErrorCode.APPLE_LINK_SESSION_CREATION_FAILED);
        }
    }

    @Override
    public Optional<AppleLinkSession> consume(String state) {
        if (state == null || state.isBlank()) {
            return Optional.empty();
        }

        // GETDEL로 조회와 삭제를 원자적으로 수행해 동시 요청 간 중복 사용 방지
        String value = redisTemplate.opsForValue().getAndDelete(linkKey(state));
        if (value == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(value, AppleLinkSession.class));
        } catch (JsonProcessingException e) {
            log.error("애플 연동 세션 역직렬화에 실패했습니다: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
