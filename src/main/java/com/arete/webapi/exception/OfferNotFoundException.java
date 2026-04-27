package com.arete.webapi.exception;

public class OfferNotFoundException extends RuntimeException {
    public OfferNotFoundException(String uuid) {
        super("No form submission found for UUID: " + uuid);
    }
}
