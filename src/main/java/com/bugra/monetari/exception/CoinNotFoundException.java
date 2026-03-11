package com.bugra.monetari.exception;

public class CoinNotFoundException extends RuntimeException {

    public CoinNotFoundException(String coinId) {
        super("Coin not found: " + coinId);
    }
}