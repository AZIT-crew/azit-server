package com.youthexpedition.azit.modules.auth.domain.model;

/**
 * 애플 계정 추가 연동을 시작한 회원을 식별하기 위한 일회용 세션.
 * 발급된 state를 애플 인증 요청에 실어 보내면 애플이 콜백으로 그대로 돌려주므로,
 * 콜백 시점에 어떤 회원의 연동 요청인지 복원할 수 있다.
 */
public record AppleLinkSession(
        Long memberId,
        String redirectUrl // 연동 완료 후 돌아갈 프론트 주소
) {}
