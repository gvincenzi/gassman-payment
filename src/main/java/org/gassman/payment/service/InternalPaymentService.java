package org.gassman.payment.service;

import org.gassman.payment.dto.UserDTO;
import org.gassman.payment.entity.RechargeUserCreditType;
import org.gassman.payment.entity.UserCredit;

import java.math.BigDecimal;

public interface InternalPaymentService {
    UserCredit userCreditUpdateCredit(UserDTO user, BigDecimal credit, RechargeUserCreditType type);
}
