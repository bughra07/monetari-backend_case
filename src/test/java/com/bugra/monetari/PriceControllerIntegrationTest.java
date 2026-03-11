package com.bugra.monetari;

import com.bugra.monetari.entity.PriceRecord;
import com.bugra.monetari.repository.PriceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PriceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PriceRecordRepository priceRecordRepository;

    @BeforeEach
    void setUp() {
        priceRecordRepository.deleteAll();

        PriceRecord record = PriceRecord.builder()
                .coinId("bitcoin")
                .price(BigDecimal.valueOf(50000))
                .currency("usd")
                .fetchedAt(LocalDateTime.now())
                .build();

        priceRecordRepository.save(record);
    }

    @Test
    void shouldReturnPriceHistoryForCoin() throws Exception {
        mockMvc.perform(
                        get("/v1/price/bitcoin/history")
                                .header("X-API-KEY", "secret-key")
                )
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("bitcoin")))
                .andExpect(content().string(containsString("50000")))
                .andExpect(content().string(containsString("usd")));
    }
}