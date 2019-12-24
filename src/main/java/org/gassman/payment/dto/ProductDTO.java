package org.gassman.payment.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
public class ProductDTO {
    private Long productId;
    private String name;
    private String description;
    private String unitOfMeasure;
    private BigDecimal pricePerUnit;
    private String deliveryDateTime;
    private Boolean active = Boolean.TRUE;
}
