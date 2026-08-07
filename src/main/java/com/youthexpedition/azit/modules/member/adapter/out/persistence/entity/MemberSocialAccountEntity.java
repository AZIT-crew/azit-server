package com.youthexpedition.azit.modules.member.adapter.out.persistence.entity;

import com.youthexpedition.azit.infrastructure.common.entity.BaseTimeEntity;
import com.youthexpedition.azit.modules.member.domain.model.enums.SocialProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_social_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 무분별한 객체 생성 제한 (JPA만 생성할 수 있도록)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 빌더로만 생성하도록 강제
@Builder
public class MemberSocialAccountEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider", nullable = false, length = 20)
    private SocialProvider socialProvider;

    @Column(name = "social_provider_id", nullable = false, length = 255)
    private String socialProviderId;

    @Column(name = "email", length = 255)
    private String email;

    @Builder.Default
    @Column(name = "is_email_sharing_enabled", nullable = false)
    private boolean isEmailSharingEnabled = true;

    @Column(name = "apple_refresh_token", length = 500)
    private String appleRefreshToken;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt; // 연동 시점
}
