package org.gassman.payment.client;

import org.gassman.payment.dto.OrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("gassman-order-service/orders")
public interface OrderResourceClient {
    @GetMapping("/{id}")
    OrderDTO findOrderById(@PathVariable Long id);
}
