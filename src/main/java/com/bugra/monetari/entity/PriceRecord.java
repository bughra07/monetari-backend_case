package com.bugra.monetari.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coin_id", nullable = false)
    private String coinId;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal price;

    @Column(nullable = false)
    private String currency;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;
}