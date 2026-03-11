package com.bugra.monetari;

import com.bugra.monetari.client.CoinGeckoClient;
import com.bugra.monetari.dto.PriceResponse;
import com.bugra.monetari.repository.PriceRecordRepository;
import com.bugra.monetari.service.PriceBatchManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PriceBatchManagerTest {

    private final CoinGeckoClient coinGeckoClient = mock(CoinGeckoClient.class);
    private final PriceRecordRepository priceRecordRepository = mock(PriceRecordRepository.class);

    private final PriceBatchManager priceBatchManager =
            new PriceBatchManager(coinGeckoClient, priceRecordRepository);

    @AfterEach
    void tearDown() {
        priceBatchManager.shutdown();
    }

    @Test
    void shouldFlushImmediatelyWhenThresholdReached() throws Exception {
        PriceResponse mockedResponse = PriceResponse.builder()
                .coinId("bitcoin")
                .price(BigDecimal.valueOf(69298))
                .currency("usd")
                .fetchedAt(LocalDateTime.now())
                .build();

        when(coinGeckoClient.fetchCurrentPrice(eq("bitcoin"), eq("usd")))
                .thenReturn(mockedResponse);

        CompletableFuture<PriceResponse> future1 = priceBatchManager.enqueue("bitcoin");
        CompletableFuture<PriceResponse> future2 = priceBatchManager.enqueue("bitcoin");
        CompletableFuture<PriceResponse> future3 = priceBatchManager.enqueue("bitcoin");

        PriceResponse response1 = future1.get(2, TimeUnit.SECONDS);
        PriceResponse response2 = future2.get(2, TimeUnit.SECONDS);
        PriceResponse response3 = future3.get(2, TimeUnit.SECONDS);

        assertEquals("bitcoin", response1.getCoinId());
        assertEquals(response1.getPrice(), response2.getPrice());
        assertEquals(response1.getPrice(), response3.getPrice());
        assertEquals(response1.getFetchedAt(), response2.getFetchedAt());
        assertEquals(response1.getFetchedAt(), response3.getFetchedAt());

        verify(coinGeckoClient, times(1)).fetchCurrentPrice("bitcoin", "usd");
        verify(priceRecordRepository, times(1)).save(any());
    }
}