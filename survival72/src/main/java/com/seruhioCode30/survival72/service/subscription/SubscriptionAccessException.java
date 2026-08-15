package com.seruhioCode30.survival72.service.subscription;

public class SubscriptionAccessException extends RuntimeException {

    public SubscriptionAccessException() {
        super("Subscription access is not available");
    }
}
