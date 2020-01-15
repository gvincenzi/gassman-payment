package org.gassman.payment.listener;

import org.gassman.payment.binding.MQBinding;
import org.gassman.payment.dto.OrderDTO;
import org.gassman.payment.dto.UserDTO;
import org.gassman.payment.entity.Order;
import org.gassman.payment.entity.Payment;
import org.gassman.payment.entity.RechargeUserCreditType;
import org.gassman.payment.entity.UserCredit;
import org.gassman.payment.repository.OrderRepository;
import org.gassman.payment.repository.PaymentRepository;
import org.gassman.payment.repository.RechargeUserCreditLogRepository;
import org.gassman.payment.repository.UserCreditRepository;
import org.gassman.payment.service.InternalPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@EnableBinding(MQBinding.class)
public class MQListener {
    @Autowired
    private InternalPaymentService internalPaymentService;

    @StreamListener(target = MQBinding.USER_REGISTRATION)
    public void processUserRegistration(UserDTO msg) {
        internalPaymentService.processUserRegistration(msg);
    }

    @StreamListener(target = MQBinding.USER_ORDER)
    public void processUserOrder(OrderDTO msg) {
        internalPaymentService.processUserOrder(msg);
    }

    @Transactional
    @StreamListener(target = MQBinding.USER_CANCELLATION)
    public void processUserCancellation(UserDTO msg) {
        internalPaymentService.processUserCancellation(msg);
    }

    @StreamListener(target = MQBinding.ORDER_CANCELLATION)
    public void processOrderCancellation(OrderDTO msg) {
        internalPaymentService.processOrderCancellation(msg);
    }
}
