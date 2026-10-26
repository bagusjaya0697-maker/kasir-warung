
package com.warung.kasir;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal payment;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal changeAmount;

    public Sale() {
    }

    public Sale(BigDecimal total, BigDecimal payment,
                BigDecimal changeAmount) {
        this.createdAt = LocalDateTime.now();
        this.total = total;
        this.payment = payment;
        this.changeAmount = changeAmount;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getPayment() {
        return payment;
    }

    public BigDecimal getChangeAmount() {
        return changeAmount;
    }
}
