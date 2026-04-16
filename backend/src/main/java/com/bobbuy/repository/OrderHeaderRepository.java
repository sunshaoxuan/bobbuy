package com.bobbuy.repository;

import com.bobbuy.model.OrderHeader;
import com.bobbuy.model.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderHeaderRepository extends JpaRepository<OrderHeader, Long> {
    Optional<OrderHeader> findByBusinessId(String businessId);

    List<OrderHeader> findByTripId(Long tripId);

    Optional<OrderHeader> findTopByOrderByIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderHeader o where o.businessId = :businessId")
    Optional<OrderHeader> findByBusinessIdForUpdate(@Param("businessId") String businessId);

    @Query("select coalesce(sum(o.totalAmount), 0) from OrderHeader o")
    double sumTotalAmount();

    @Query("select o.status as status, count(o) as total from OrderHeader o group by o.status")
    List<OrderStatusCountView> countByStatus();

    interface OrderStatusCountView {
        OrderStatus getStatus();

        long getTotal();
    }
}
