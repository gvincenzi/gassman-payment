package org.gassman.payment.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gassman_user_credit")
public class UserCredit {
    @Id
    private Long userId;
    @Column
    private String name;
    @Column
    private String surname;
    @Column
    private String mail;
    @Column
    private Integer telegramUserId;
    @Column
    private BigDecimal credit;
}
