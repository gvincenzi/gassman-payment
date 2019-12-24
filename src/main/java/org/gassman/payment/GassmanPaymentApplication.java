package org.gassman.payment;

import org.gassman.payment.binding.MQBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.stream.annotation.EnableBinding;

@EnableBinding(MQBinding.class)
@EnableEurekaClient
@SpringBootApplication
public class GassmanPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(GassmanPaymentApplication.class, args);
    }

}
