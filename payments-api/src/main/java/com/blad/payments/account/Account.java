package com.blad.payments.account;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 120)
    private String ownerName;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}