package com.tfs.training.paymentservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @Entity tells Hibernate: "This class represents a database table!"
 * @Table(name = "payments") tells Hibernate exactly what to name the table.
 * @Data is our Lombok robot. It automatically writes the getters (getId(), getAmount()) and setters behind the scenes.
 */
@Entity
@Table(name = "payments")
@Data
public class Payment {

    /**
     * @Id tells Hibernate this is the Primary Key (the unique identifier for a row).
     * @GeneratedValue tells the database to automatically generate this number (like 1, 2, 3...) when we save a new row.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // We make sure this column can't be empty, and it must be unique.
    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private String loanAccountNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
