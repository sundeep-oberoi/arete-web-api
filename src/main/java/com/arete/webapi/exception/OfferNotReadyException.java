package com.arete.webapi.exception;

public class OfferNotReadyException extends RuntimeException {
    public OfferNotReadyException(String uuid) {
        super("Offer not yet computed for UUID: " + uuid);
    }
}
