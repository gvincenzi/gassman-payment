package org.gassman.payment.repository;

import org.gassman.payment.entity.UserCredit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCreditRepository extends JpaRepository<UserCredit, Long> {
}
