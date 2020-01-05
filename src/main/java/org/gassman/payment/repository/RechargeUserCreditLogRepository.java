package org.gassman.payment.repository;

import org.gassman.payment.entity.RechargeUserCreditLog;
import org.gassman.payment.entity.UserCredit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RechargeUserCreditLogRepository extends JpaRepository<RechargeUserCreditLog, Long> {
    void deleteAllByUserCredit(UserCredit userCredit);
}
