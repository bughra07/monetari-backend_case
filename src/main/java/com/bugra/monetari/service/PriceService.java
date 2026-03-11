package com.bugra.monetari.service;

import com.bugra.monetari.dto.PriceHistoryItemResponse;
import com.bugra.monetari.dto.PriceResponse;
import com.bugra.monetari.repository.PriceRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class PriceService {

    private static final Logger log = LoggerFactory.getLogger(PriceService.class);

    private final PriceBatchManager priceBatchManager;
    private final PriceRecordRepository priceRecordRepository;

    public PriceService(PriceBatchManager priceBatchManager,
                        PriceRecordRepository priceRecordRepository) {
        this.priceBatchManager = priceBatchManager;
        this.priceRecordRepository = priceRecordRepository;
    }

    public CompletableFuture<PriceResponse> getCurrentPrice(String coinId) {
        log.info("Received current price request for coinId={}", coinId);
        return priceBatchManager.enqueue(coinId);
    }

    public List<PriceHistoryItemResponse> getPriceHistory(String coinId) {
        log.info("Fetching price history for coinId={}", coinId);

        return priceRecordRepository.findByCoinIdOrderByFetchedAtDesc(coinId)
                .stream()
                .map(record -> PriceHistoryItemResponse.builder()
                        .coinId(record.getCoinId())
                        .price(record.getPrice())
                        .currency(record.getCurrency())
                        .fetchedAt(record.getFetchedAt())
                        .build())
                .toList();
    }
}