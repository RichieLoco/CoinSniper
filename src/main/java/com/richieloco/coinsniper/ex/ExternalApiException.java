package com.richieloco.coinsniper.ex;

import lombok.Getter;

@Getter
@SuppressWarnings("serial")
public class ExternalApiException extends RuntimeException {
    private final int statusCode;

    public ExternalApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

}