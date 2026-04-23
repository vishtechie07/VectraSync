package com.vectrasync.crm;

import java.time.Duration;
import java.util.Optional;

public class HttpStatusException extends RuntimeException {

    private final int status;
    private final Duration retryAfter;
    private final String body;

    public HttpStatusException(int status, Duration retryAfter, String body) {
        super("HTTP " + status + (body == null ? "" : ": " + truncate(body)));
        this.status = status;
        this.retryAfter = retryAfter;
        this.body = body;
    }

    public int status() {
        return status;
    }

    public Optional<Duration> retryAfter() {
        return Optional.ofNullable(retryAfter);
    }

    public String body() {
        return body;
    }

    public boolean isRateLimited() {
        return status == 429;
    }

    public boolean isUnauthorized() {
        return status == 401 || status == 403;
    }

    public boolean isServerError() {
        return status >= 500;
    }

    private static String truncate(String s) {
        return s.length() <= 256 ? s : s.substring(0, 256) + "...";
    }
}
