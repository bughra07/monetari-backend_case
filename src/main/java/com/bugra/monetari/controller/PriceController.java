package com.bugra.monetari.controller;

import com.bugra.monetari.dto.PriceHistoryItemResponse;
import com.bugra.monetari.dto.PriceResponse;
import com.bugra.monetari.service.PriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RequestMapping("/v1/price")
@RestController
@SecurityRequirement(name = "ApiKeyAuth")
public class PriceController {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @Operation(summary = "Get current price for a coin", description = "Returns current price using batching logic")
    @GetMapping("/{coinId}")
    public CompletableFuture<PriceResponse> getCurrentPrice(
            @Parameter(description = "Coin identifier, e.g. bitcoin") @PathVariable String coinId) {
        return priceService.getCurrentPrice(coinId);
    }

    @Operation(summary = "Get price history for a coin", description = "Returns stored historical price records")
    @GetMapping("/{coinId}/history")
    public List<PriceHistoryItemResponse> getPriceHistory(
            @Parameter(description = "Coin identifier, e.g. bitcoin") @PathVariable String coinId) {
        return priceService.getPriceHistory(coinId);
    }
}