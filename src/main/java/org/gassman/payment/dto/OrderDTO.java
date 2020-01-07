package org.gassman.payment.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class OrderDTO extends InternalOrderDTO {
    private ProductDTO product;
}
