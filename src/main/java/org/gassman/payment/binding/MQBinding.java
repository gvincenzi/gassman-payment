package org.gassman.payment.binding;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface MQBinding {
    String ORDER_PAYMENT = "orderPaymentChannel";

    @Output(ORDER_PAYMENT)
    MessageChannel orderPaymentChannel();
}
