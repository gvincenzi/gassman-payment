package org.gassman.payment.binding;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface MQBinding {
    String ORDER_PAYMENT = "orderPaymentChannel";
    String RECHARGE_USER_CREDIT = "rechargeUserCreditChannel";

    @Output(ORDER_PAYMENT)
    MessageChannel orderPaymentChannel();

    @Output(RECHARGE_USER_CREDIT)
    MessageChannel rechargeUserCreditChannel();
}
