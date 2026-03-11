package com.bugra.monetari;

import com.bugra.monetari.dto.PriceHistoryItemResponse;
import com.bugra.monetari.entity.PriceRecord;
import com.bugra.monetari.repository.PriceRecordRepository;
import com.bugra.monetari.service.PriceBatchManager;
import com.bugra.monetari.service.PriceService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PriceServiceTest {

    private final PriceBatchManager priceBatchManager = mock(PriceBatchManager.class);
    private final PriceRecordRepository priceRecordRepository = mock(PriceRecordRepository.class);

    private final PriceService priceService =
            new PriceService(priceBatchManager, priceRecordRepository);

    @Test
    void shouldReturnMappedPriceHistory() {
        PriceRecord record1 = PriceRecord.builder()
                .coinId("bitcoin")
                .price(BigDecimal.valueOf(70000))
                .currency("usd")
                .fetchedAt(LocalDateTime.of(2026, 3, 11, 15, 0))
                .build();

        PriceRecord record2 = PriceRecord.builder()
                .coinId("bitcoin")
                .price(BigDecimal.valueOf(69000))
                .currency("usd")
                .fetchedAt(LocalDateTime.of(2026, 3, 11, 14, 0))
                .build();

        when(priceRecordRepository.findByCoinIdOrderByFetchedAtDesc("bitcoin"))
                .thenReturn(List.of(record1, record2));

        List<PriceHistoryItemResponse> history = priceService.getPriceHistory("bitcoin");

        assertEquals(2, history.size());
        assertEquals("bitcoin", history.get(0).getCoinId());
        assertEquals(BigDecimal.valueOf(70000), history.get(0).getPrice());
        assertEquals("usd", history.get(0).getCurrency());
        assertEquals(LocalDateTime.of(2026, 3, 11, 15, 0), history.get(0).getFetchedAt());

        verify(priceRecordRepository, times(1))
                .findByCoinIdOrderByFetchedAtDesc("bitcoin");
    }
}