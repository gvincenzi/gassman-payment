package org.gassman.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class OrderDTO {
    private Long orderId;
    private Integer quantity;
    private UserDTO user;
    private ProductDTO product;

    @JsonIgnore
    public BigDecimal computeTotalToPay() {
        return getQuantity() != null && getProduct() != null
                && getProduct().getPricePerUnit() != null ? new BigDecimal(getQuantity()).multiply(getProduct().getPricePerUnit()) : BigDecimal.ZERO;
    }
}
