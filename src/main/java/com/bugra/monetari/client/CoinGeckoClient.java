package com.bugra.monetari.client;

import com.bugra.monetari.dto.PriceResponse;
import com.bugra.monetari.exception.CoinNotFoundException;
import com.bugra.monetari.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class CoinGeckoClient {

    private static final Logger log = LoggerFactory.getLogger(CoinGeckoClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CoinGeckoClient(@Value("${app.coingecko.base-url}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
    }

    public PriceResponse fetchCurrentPrice(String coinId, String currency) {
        try {
            log.info("Fetching current price from CoinGecko for coinId={}, currency={}", coinId, currency);

            String url = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/simple/price")
                    .queryParam("ids", coinId)
                    .queryParam("vs_currencies", currency)
                    .toUriString();

            Map<String, Map<String, BigDecimal>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Map<String, BigDecimal>>>() {}
            ).getBody();

            if (response == null || !response.containsKey(coinId)) {
                throw new CoinNotFoundException(coinId);
            }

            Map<String, BigDecimal> priceMap = response.get(coinId);

            if (priceMap == null || !priceMap.containsKey(currency)) {
                throw new ExternalApiException("Price data not found for coin: " + coinId);
            }

            BigDecimal price = priceMap.get(currency);

            return PriceResponse.builder()
                    .coinId(coinId)
                    .price(price)
                    .currency(currency)
                    .fetchedAt(LocalDateTime.now())
                    .build();

        } catch (RestClientException e) {
            log.error("Failed to fetch price from CoinGecko for coinId={}, currency={}", coinId, currency, e);
            throw new ExternalApiException("Failed to fetch price from CoinGecko", e);
        }
    }
}