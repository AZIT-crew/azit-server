package com.youthexpedition.azit.modules.member.adapter.out.persistence.entity;

import com.youthexpedition.azit.infrastructure.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "member_crew_notification_setting",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_member_crew_notification",
                columnNames = {"member_id", "crew_id"}
        ),
        indexes = @Index(name = "idx_member_crew_notification_crew_id", columnList = "crew_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 무분별한 객체 생성 제한 (JPA만 생성할 수 있도록)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 빌더로만 생성하도록 강제
@Builder
public class MemberCrewNotificationSettingEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "crew_id", nullable = false)
    private Long crewId;

    @Builder.Default
    @Column(name = "is_regular_run_enabled", nullable = false)
    private boolean isRegularRunEnabled = true; // 정기런 알림

    @Builder.Default
    @Column(name = "is_lightning_run_enabled", nullable = false)
    private boolean isLightningRunEnabled = true; // 번개런 알림
}
