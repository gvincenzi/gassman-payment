package org.gassman.payment.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "gassman_order")
public class Order {
    @Id
    private Long orderId;
    @Column
    private BigDecimal totalToPay;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="userId")
    private UserCredit userCredit;
}
