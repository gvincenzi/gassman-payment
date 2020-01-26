package org.gassman.payment.client;

import org.gassman.payment.configuration.FeignClientConfiguration;
import org.gassman.payment.dto.OrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "gassman-order-service/orders", configuration = FeignClientConfiguration.class)
public interface OrderResourceClient {
    @GetMapping("/{id}")
    OrderDTO findOrderById(@PathVariable Long id);
}
