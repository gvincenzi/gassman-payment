package org.gassman.payment.service;

import org.gassman.payment.dto.OrderDTO;
import org.gassman.payment.dto.UserDTO;
import org.gassman.payment.entity.Order;
import org.gassman.payment.entity.RechargeUserCreditType;
import org.gassman.payment.entity.UserCredit;

import java.math.BigDecimal;

public interface InternalPaymentService {
    UserCredit userCreditUpdateCredit(UserDTO user, BigDecimal credit, RechargeUserCreditType type);
    void processUserRegistration(UserDTO msg);
    Order processUserOrder(OrderDTO msg);
    void processUserCancellation(UserDTO msg);
    void processOrderCancellation(OrderDTO msg);
}
