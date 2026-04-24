package com.smartcampus.api.service.ai;

public class RateLimitException extends RuntimeException {

    private final Integer retryAfterSeconds;

    public RateLimitException(Integer retryAfterSeconds, String body) {
        super(body);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
