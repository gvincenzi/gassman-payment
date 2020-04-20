package org.gassman.payment.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "gassman_payment")
public class Payment {
    @Id
    private String paymentId;
    @Column
    private LocalDateTime paymentDateTime;
    @Column
    private Long orderId;
    @Column
    private PaymentType paymentType;
    @Column
    private BigDecimal amount;
}
