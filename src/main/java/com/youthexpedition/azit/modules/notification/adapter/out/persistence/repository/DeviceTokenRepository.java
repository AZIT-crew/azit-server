package com.youthexpedition.azit.modules.notification.adapter.out.persistence.repository;

import com.youthexpedition.azit.modules.notification.adapter.out.persistence.entity.DeviceTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceTokenEntity, Long> {

    Optional<DeviceTokenEntity> findByToken(String token);

    List<DeviceTokenEntity> findAllByMemberIdIn(Collection<Long> memberIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM DeviceTokenEntity d WHERE d.token = :token")
    void deleteByToken(@Param("token") String token);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM DeviceTokenEntity d WHERE d.token IN :tokens")
    void deleteAllByTokenIn(@Param("tokens") Collection<String> tokens);
}
