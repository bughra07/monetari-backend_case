package com.bugra.monetari.service;

import com.bugra.monetari.client.CoinGeckoClient;
import com.bugra.monetari.dto.PriceResponse;
import com.bugra.monetari.entity.PriceRecord;
import com.bugra.monetari.repository.PriceRecordRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class PriceBatchManager {

    private static final Logger log = LoggerFactory.getLogger(PriceBatchManager.class);

    private static final int THRESHOLD = 3;
    private static final int WAIT_SECONDS = 5;
    private static final String DEFAULT_CURRENCY = "usd";

    private final CoinGeckoClient coinGeckoClient;
    private final PriceRecordRepository priceRecordRepository;

    private final Map<String, BatchState> batches = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public PriceBatchManager(CoinGeckoClient coinGeckoClient,
                             PriceRecordRepository priceRecordRepository) {
        this.coinGeckoClient = coinGeckoClient;
        this.priceRecordRepository = priceRecordRepository;
    }

    public CompletableFuture<PriceResponse> enqueue(String coinId) {
        CompletableFuture<PriceResponse> future = new CompletableFuture<>();

        BatchState batchState = batches.computeIfAbsent(coinId, key -> {
            log.info("Creating new batch for coin={}", key);

            BatchState newBatch = new BatchState();

            ScheduledFuture<?> scheduledFuture = scheduler.schedule(
                    () -> flushBatch(key),
                    WAIT_SECONDS,
                    TimeUnit.SECONDS
            );

            log.info("Scheduled timeout flush for coin={} after {} seconds", key, WAIT_SECONDS);

            newBatch.setScheduledTask(scheduledFuture);
            return newBatch;
        });

        batchState.getWaitingRequests().add(future);

        log.info("Request queued for coin={}, currentSize={}, thread={}",
                coinId,
                batchState.getWaitingRequests().size(),
                Thread.currentThread().getName());

        if (batchState.getWaitingRequests().size() >= THRESHOLD) {
            log.info("Threshold reached for coin={}, size={}",
                    coinId,
                    batchState.getWaitingRequests().size());
            flushBatch(coinId);
        }

        return future;
    }

    private void flushBatch(String coinId) {
        log.info("flushBatch called for coin={}, thread={}",
                coinId,
                Thread.currentThread().getName());

        BatchState batchState = batches.get(coinId);

        if (batchState == null) {
            log.warn("No batch found for coin={}", coinId);
            return;
        }

        AtomicBoolean flushed = batchState.getFlushed();
        if (!flushed.compareAndSet(false, true)) {
            log.warn("Flush already executed for coin={}", coinId);
            return;
        }

        ScheduledFuture<?> scheduledTask = batchState.getScheduledTask();
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false);
            log.info("Cancelled scheduled task for coin={}", coinId);
        }

        try {
            log.info("Calling external API for coin={}", coinId);

            PriceResponse priceResponse = coinGeckoClient.fetchCurrentPrice(coinId, DEFAULT_CURRENCY);

            PriceRecord priceRecord = PriceRecord.builder()
                    .coinId(priceResponse.getCoinId())
                    .price(priceResponse.getPrice())
                    .currency(priceResponse.getCurrency())
                    .fetchedAt(priceResponse.getFetchedAt())
                    .build();

            priceRecordRepository.save(priceRecord);

            log.info("Saved price record for coin={}, waitingRequestCount={}",
                    coinId,
                    batchState.getWaitingRequests().size());

            log.info("Completing all waiting requests for coin={}", coinId);

            for (CompletableFuture<PriceResponse> waitingRequest : batchState.getWaitingRequests()) {
                waitingRequest.complete(priceResponse);
            }
        } catch (Exception ex) {
            log.error("Error while flushing batch for coin={}", coinId, ex);

            for (CompletableFuture<PriceResponse> waitingRequest : batchState.getWaitingRequests()) {
                waitingRequest.completeExceptionally(ex);
            }
        } finally {
            log.info("Removing batch for coin={}", coinId);
            batches.remove(coinId);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down scheduler");
        scheduler.shutdown();
    }
}