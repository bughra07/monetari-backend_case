package com.bugra.monetari.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceResponse {

    private String coinId;
    private BigDecimal price;
    private String currency;
    private LocalDateTime fetchedAt;
}