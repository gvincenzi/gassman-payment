package org.gassman.payment.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class InternalOrderDTO {
    private Long orderId;
    private BigDecimal totalToPay;
    private Integer quantity;
    private UserDTO user;
}
