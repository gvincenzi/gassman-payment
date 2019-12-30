package org.gassman.payment.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class OrderDTO {
    private Long orderId;
    private BigDecimal totalToPay;
    private Integer quantity;
    private ProductDTO product;
    private UserDTO user;
}
