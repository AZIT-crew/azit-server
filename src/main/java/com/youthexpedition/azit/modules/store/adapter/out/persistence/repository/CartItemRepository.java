package com.youthexpedition.azit.modules.store.adapter.out.persistence.repository;

import com.youthexpedition.azit.modules.store.adapter.out.persistence.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItemEntity, Long>, CartItemRepositoryCustom {
    @Query("SELECT ci FROM CartItemEntity ci " +
            "JOIN FETCH ci.product p " +
            "JOIN FETCH ci.sku s " +
            "WHERE ci.memberId = :memberId AND ci.sku.id = :skuId")
    Optional<CartItemEntity> findByMemberIdAndSkuId(Long memberId, Long skuId);

    @Modifying
    @Query("UPDATE CartItemEntity ci SET ci.quantity = ci.quantity + :quantity WHERE ci.id = :id")
    void addQuantity(@Param("id") Long id, @Param("quantity") int quantity);

    /*
     * flushAutomatically = true 가 반드시 필요함.
     * 주문 생성 시 회원 포인트 차감(member UPDATE) 직후에 호출되는데, 대상 테이블이 달라
     * Hibernate 오토 플러시가 걸리지 않음. flush 없이 clearAutomatically 로 영속성 컨텍스트를 비우면
     * 아직 flush되지 않은 포인트 차감이 유실되어 주문은 저장되고 포인트만 안 깎이는 상태가 됨.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CartItemEntity ci WHERE ci.memberId = :memberId AND ci.id IN :ids")
    void deleteAllByMemberIdAndIds(@Param("memberId") Long memberId, @Param("ids") List<Long> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CartItemEntity ci WHERE ci.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);

    long countByMemberId(Long memberId);
}
