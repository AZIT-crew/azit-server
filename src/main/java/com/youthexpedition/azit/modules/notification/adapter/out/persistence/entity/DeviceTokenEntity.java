package com.youthexpedition.azit.modules.notification.adapter.out.persistence.entity;

import com.youthexpedition.azit.infrastructure.common.entity.BaseTimeEntity;
import com.youthexpedition.azit.modules.notification.domain.model.enums.DeviceType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "member_device_token",
        uniqueConstraints = @UniqueConstraint(name = "uq_member_device_token", columnNames = "token"),
        indexes = @Index(name = "idx_member_device_token_member_id", columnList = "member_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DeviceTokenEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "token", nullable = false, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;
}
