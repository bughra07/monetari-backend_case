Monetari Backend Case

A backend service that fetches cryptocurrency prices from the CoinGecko API and exposes them through REST endpoints.

The system is designed to efficiently handle high traffic by batching concurrent requests for the same cryptocurrency, minimizing unnecessary external API calls.

The application is built using Java Spring Boot, PostgreSQL, and Docker.

FEATURES

Fetch cryptocurrency price from the CoinGecko API

Batch multiple requests for the same coin

Threshold-based batching

Timeout-based batching (5 seconds)

Store fetched prices in PostgreSQL

Retrieve price history

API key based authentication

Global exception handling

Structured JSON logging

Unit testing

Integration testing

Dockerized deployment

Swagger API documentation

ARCHITECTURE OVERVIEW

The application follows a layered architecture:

Controller → Service → Batch Manager → External API Client → Database

REQUEST FLOW

Client sends request to fetch coin price

Request reaches PriceController

PriceService forwards the request to PriceBatchManager

Batch manager groups requests for the same coin

Batch is triggered when:

3 requests arrive

5 seconds pass

A single request is sent to CoinGecko API

The fetched price is stored in PostgreSQL

All waiting requests receive the same response

This design reduces redundant external API calls and improves system efficiency.

API ENDPOINTS

GET CURRENT PRICE

GET /v1/price/{coinId}

Example request:

GET /v1/price/bitcoin

Header:

X-API-KEY: secret-key

Response example:

{
"coinId": "bitcoin",
"price": 69298,
"currency": "usd",
"fetchedAt": "2026-03-11T12:43:16"
}

GET PRICE HISTORY

GET /v1/price/{coinId}/history

Example request:

GET /v1/price/bitcoin/history

Header:

X-API-KEY: secret-key

Response example:

[
{
"coinId": "bitcoin",
"price": 69298,
"currency": "usd",
"fetchedAt": "2026-03-11T12:43:16"
}
]

AUTHENTICATION

All endpoints require an API key.

Header:

X-API-KEY: secret-key

Requests without a valid API key return:

401 Unauthorized

RUNNING THE PROJECT

OPTION 1 — RUN WITH DOCKER

Requirements:

Docker
Docker Compose

Start the application:

docker-compose up --build

Services started:

Spring Boot API
PostgreSQL database

API will be available at:

http://localhost:8080

OPTION 2 — RUN LOCALLY

Requirements:

Java 21
Maven
PostgreSQL

Run the project:

./mvnw spring-boot:run

SWAGGER DOCUMENTATION

Swagger UI:

http://localhost:8080/swagger-ui/index.html

TESTING

Run tests with:

./mvnw test

Implemented tests include:

Unit test for batch threshold behavior

Unit test for price history retrieval

Integration test for price history endpoint

LOGGING

The application uses structured JSON logging.

Example log entry:

{
"@timestamp": "2026-03-11T13:53:21.883Z",
"level": "INFO",
"logger_name": "PriceBatchManager",
"message": "Creating new batch for coin=bitcoin"
}

TECHNOLOGIES USED

Java 21
Spring Boot
PostgreSQL
Maven
Docker
Swagger (OpenAPI)
JUnit
Mockito
Logstash Logback Encoder

AUTHOR

Muhammed Buğrahan Terlik
