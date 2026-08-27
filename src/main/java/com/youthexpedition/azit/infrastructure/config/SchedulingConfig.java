package com.youthexpedition.azit.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 알림 아웃박스 폴러 등 주기 작업 활성화.
 * 폴러는 FOR UPDATE SKIP LOCKED 로 처리 대상을 선점하므로 인스턴스가 여러 대여도 별도 분산 락이 필요 없음.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
