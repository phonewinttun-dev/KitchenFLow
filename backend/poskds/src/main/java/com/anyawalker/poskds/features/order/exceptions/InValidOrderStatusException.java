package com.anyawalker.poskds.features.order.exceptions;

public class InValidOrderStatusException extends RuntimeException {
    public InValidOrderStatusException(String message) {
        super(message);
    }
}
