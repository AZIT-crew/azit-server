package com.youthexpedition.azit.modules.member.domain.model;

import com.youthexpedition.azit.infrastructure.exception.BusinessException;
import com.youthexpedition.azit.modules.member.domain.model.enums.MemberErrorCode;
import com.youthexpedition.azit.modules.member.domain.model.enums.MemberStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DisplayName("Member 도메인 단위 테스트")
class MemberTest {

    @Test
    @DisplayName("성공: 닉네임이 정상적으로 변경된다.")
    void updateNickname_success() {
        // given
        Member member = Member.create("oldNickname", "test@example.com", "imageUrl");

        // when
        member.updateNickname("newNickname");

        // then
        assertThat(member.getNickname()).isEqualTo("newNickname");
    }

    @Test
    @DisplayName("성공: 닉네임을 동일한 값으로 변경해도 정상 처리된다.")
    void updateNickname_sameValue() {
        // given
        Member member = Member.create("sameNickname", "test@example.com", "imageUrl");

        // when
        member.updateNickname("sameNickname");

        // then
        assertThat(member.getNickname()).isEqualTo("sameNickname");
    }

    @Test
    @DisplayName("성공: 탈퇴 시 상태가 WITHDRAWN으로 변경되고 탈퇴 시점이 기록된다.")
    void withdraw_success_recordsWithdrawnAt() {
        // given
        Member member = Member.create("nickname", "test@example.com", "imageUrl");
        LocalDateTime withdrawnAt = LocalDateTime.of(2026, 7, 9, 12, 0);

        // when
        member.withdraw(withdrawnAt);

        // then
        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(member.getWithdrawnAt()).isEqualTo(withdrawnAt);
    }

    @Test
    @DisplayName("성공: 유예기간 내 재활성화 시 ACTIVE 상태가 되고 탈퇴 시점이 초기화된다.")
    void reactivate_success_clearsWithdrawnAt() {
        // given
        Member member = Member.create("nickname", "test@example.com", "imageUrl");
        LocalDateTime withdrawnAt = LocalDateTime.of(2026, 7, 9, 12, 0);
        member.withdraw(withdrawnAt);

        // when - 탈퇴 후 10일 뒤 재로그인
        member.reactivate(withdrawnAt.plusDays(10));

        // then
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.getWithdrawnAt()).isNull();
    }

    @Test
    @DisplayName("실패: 유예기간(30일)이 만료된 회원은 재활성화할 수 없다.")
    void reactivate_throwsException_whenGracePeriodExpired() {
        // given
        Member member = Member.create("nickname", "test@example.com", "imageUrl");
        LocalDateTime withdrawnAt = LocalDateTime.of(2026, 7, 9, 12, 0);
        member.withdraw(withdrawnAt);

        // when & then - 탈퇴 후 31일 뒤 재로그인
        assertThatThrownBy(() -> member.reactivate(withdrawnAt.plusDays(31)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.WITHDRAWAL_GRACE_PERIOD_EXPIRED);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("실패: 이미 파기 완료된(DELETED) 회원은 재활성화할 수 없다.")
    void reactivate_throwsException_whenDeleted() {
        // given
        Member member = Member.builder()
                .status(MemberStatus.DELETED)
                .build();

        // when & then
        assertThatThrownBy(() -> member.reactivate(LocalDateTime.of(2026, 7, 9, 12, 0)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.WITHDRAWAL_GRACE_PERIOD_EXPIRED);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.DELETED);
    }

    @Test
    @DisplayName("성공: 유예기간 마지막 날(30일째)까지는 재활성화할 수 있다.")
    void reactivate_success_onLastDayOfGracePeriod() {
        // given
        Member member = Member.create("nickname", "test@example.com", "imageUrl");
        LocalDateTime withdrawnAt = LocalDateTime.of(2026, 7, 9, 12, 0);
        member.withdraw(withdrawnAt);

        // when - 정확히 30일 뒤 재로그인
        member.reactivate(withdrawnAt.plusDays(30));

        // then
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("성공: 탈퇴 상태가 아닌 회원은 재활성화 호출 시 상태가 변경되지 않는다.")
    void reactivate_noChange_whenNotWithdrawn() {
        // given
        Member member = Member.create("nickname", "test@example.com", "imageUrl");

        // when
        member.reactivate(LocalDateTime.of(2026, 7, 9, 12, 0));

        // then
        assertThat(member.getStatus()).isEqualTo(MemberStatus.PENDING_TERMS);
    }

    @Test
    @DisplayName("성공: 마케팅 정보 수신에 동의하면 동의 시점이 기록된다.")
    void updateMarketingConsent_success_whenAgreed() {
        // given
        Member member = Member.create("nickname", "test@example.com", "imageUrl");
        LocalDateTime now = LocalDateTime.of(2026, 8, 18, 10, 0);

        // when
        member.updateMarketingConsent(true, now);

        // then
        assertThat(member.isMarketingTermsAgreed()).isTrue();
        assertThat(member.getMarketingTermsAgreedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("성공: 마케팅 정보 수신을 거부하면 동의 시점이 비워진다.")
    void updateMarketingConsent_success_whenDisagreed() {
        // given - 이미 동의한 회원
        Member member = Member.create("nickname", "test@example.com", "imageUrl");
        member.updateMarketingConsent(true, LocalDateTime.of(2026, 8, 18, 10, 0));

        // when
        member.updateMarketingConsent(false, LocalDateTime.of(2026, 8, 19, 10, 0));

        // then
        assertThat(member.isMarketingTermsAgreed()).isFalse();
        assertThat(member.getMarketingTermsAgreedAt()).isNull();
    }

    @Test
    @DisplayName("성공: 알림 수신에 동의하면 동의 시점이 기록되고, 거부하면 비워진다.")
    void updateNotificationConsent_success() {
        // given
        Member member = Member.create("nickname", "test@example.com", "imageUrl");
        LocalDateTime now = LocalDateTime.of(2026, 8, 18, 10, 0);

        // when
        member.updateNotificationConsent(true, now);

        // then
        assertThat(member.isNotificationAgreed()).isTrue();
        assertThat(member.getNotificationAgreedAt()).isEqualTo(now);

        // when - 거부로 변경
        member.updateNotificationConsent(false, now.plusDays(1));

        // then
        assertThat(member.isNotificationAgreed()).isFalse();
        assertThat(member.getNotificationAgreedAt()).isNull();
    }
}
