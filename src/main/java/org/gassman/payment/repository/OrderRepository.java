package org.gassman.payment.repository;

import org.gassman.payment.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserCreditUserId(Long userId);
}
