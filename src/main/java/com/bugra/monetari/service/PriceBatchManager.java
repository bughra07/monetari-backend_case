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

        while (true) {
            BatchState batchState = batches.computeIfAbsent(coinId, key -> {
                log.info("Creating new batch for coin={}", key);

                BatchState newBatch = new BatchState();

                ScheduledFuture<?> scheduledFuture = scheduler.schedule(
                        () -> flushBatch(key),
                        WAIT_SECONDS,
                        TimeUnit.SECONDS
                );

                newBatch.setScheduledTask(scheduledFuture);

                log.info("Scheduled timeout flush for coin={} after {} seconds", key, WAIT_SECONDS);
                return newBatch;
            });

            synchronized (batchState) {
                if (batchState.getFlushed().get()) {
                    log.warn("Batch already flushed for coin={}, retrying enqueue with a new batch", coinId);
                    continue;
                }

                batchState.getWaitingRequests().add(future);

                log.info("Request queued for coin={}, currentSize={}, thread={}",
                        coinId,
                        batchState.getWaitingRequests().size(),
                        Thread.currentThread().getName());

                if (batchState.getWaitingRequests().size() >= THRESHOLD) {
                    log.info("Threshold reached for coin={}, size={}",
                            coinId,
                            batchState.getWaitingRequests().size());
                    flushBatchInternal(coinId, batchState);
                }
            }

            return future;
        }
    }

    private void flushBatch(String coinId) {
        BatchState batchState = batches.get(coinId);

        if (batchState == null) {
            log.warn("No batch found for coin={}", coinId);
            return;
        }

        synchronized (batchState) {
            log.info("flushBatch called for coin={}, thread={}",
                    coinId,
                    Thread.currentThread().getName());
            flushBatchInternal(coinId, batchState);
        }
    }

    private void flushBatchInternal(String coinId, BatchState batchState) {
        if (!batchState.getFlushed().compareAndSet(false, true)) {
            log.warn("Flush already executed for coin={}", coinId);
            return;
        }

        ScheduledFuture<?> scheduledTask = batchState.getScheduledTask();
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false);
            log.info("Cancelled scheduled task for coin={}", coinId);
        }

        batches.remove(coinId);
        log.info("Removed batch from map for coin={}", coinId);

        try {
            log.info("Calling external API for coin={}", coinId);

            PriceResponse response = coinGeckoClient.fetchCurrentPrice(coinId, DEFAULT_CURRENCY);

            PriceRecord record = PriceRecord.builder()
                    .coinId(response.getCoinId())
                    .price(response.getPrice())
                    .currency(response.getCurrency())
                    .fetchedAt(response.getFetchedAt())
                    .build();

            priceRecordRepository.save(record);

            log.info("Saved price record for coin={}, waitingRequestCount={}",
                    coinId,
                    batchState.getWaitingRequests().size());

            for (CompletableFuture<PriceResponse> req : batchState.getWaitingRequests()) {
                req.complete(response);
            }

            log.info("Completed all waiting requests for coin={}", coinId);

        } catch (Exception ex) {
            log.error("Error while flushing batch for coin={}", coinId, ex);

            for (CompletableFuture<PriceResponse> req : batchState.getWaitingRequests()) {
                req.completeExceptionally(ex);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down scheduler");
        scheduler.shutdown();
    }
}