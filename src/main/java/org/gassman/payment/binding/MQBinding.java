package org.gassman.payment.binding;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface MQBinding {
    String ORDER_PAYMENT = "orderPaymentChannel";
    String RECHARGE_USER_CREDIT = "rechargeUserCreditChannel";
    String USER_REGISTRATION = "userRegistrationChannel";

    @Output(ORDER_PAYMENT)
    MessageChannel orderPaymentChannel();

    @Output(RECHARGE_USER_CREDIT)
    MessageChannel rechargeUserCreditChannel();

    @Input(USER_REGISTRATION)
    SubscribableChannel userRegistrationChannel();
}
