package org.gassman.payment.binding;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface MQBinding {
    String ORDER_PAYMENT = "orderPaymentChannel";
    String RECHARGE_USER_CREDIT = "rechargeUserCreditChannel";
    String USER_REGISTRATION = "userRegistrationChannel";
    String USER_ORDER = "userOrderChannel";
    String USER_CANCELLATION = "userCancellationChannel";
    String ORDER_CANCELLATION = "orderCancellationChannel";

    @Output(ORDER_PAYMENT)
    MessageChannel orderPaymentChannel();

    @Output(RECHARGE_USER_CREDIT)
    MessageChannel rechargeUserCreditChannel();

    @Input(USER_REGISTRATION)
    SubscribableChannel userRegistrationChannel();

    @Input(USER_ORDER)
    SubscribableChannel userOrderChannel();

    @Input(USER_CANCELLATION)
    SubscribableChannel userCancellationChannel();

    @Input(ORDER_CANCELLATION)
    SubscribableChannel orderCancellationChannel();
}
