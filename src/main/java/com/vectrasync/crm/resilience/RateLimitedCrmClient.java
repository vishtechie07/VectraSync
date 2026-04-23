package com.vectrasync.crm.resilience;

import com.vectrasync.crm.Contact;
import com.vectrasync.crm.CrmClient;
import com.vectrasync.crm.FieldSchema;
import com.vectrasync.crm.HttpStatusException;
import com.vectrasync.crm.UpsertResult;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class RateLimitedCrmClient implements CrmClient {

    private final CrmClient delegate;
    private final RateLimiter rateLimiter;
    private final Retry retry;

    public RateLimitedCrmClient(CrmClient delegate, RateLimiter rateLimiter, Retry retry) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
        this.retry = Objects.requireNonNull(retry, "retry");
    }

    public static RateLimitedCrmClient wrap(CrmClient delegate, int requestsPerSecond) {
        RateLimiter rl = RateLimiter.of(delegate.name() + "-rl", RateLimiterConfig.custom()
                .limitForPeriod(requestsPerSecond)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofSeconds(30))
                .build());
        Retry rt = Retry.of(delegate.name() + "-retry", RetryConfig.<Object>custom()
                .maxAttempts(4)
                .retryOnException(ex -> ex instanceof HttpStatusException hse
                        && (hse.isRateLimited() || hse.isServerError()))
                .intervalFunction(attempt -> 500L * (1L << (attempt - 1)))
                .build());
        return new RateLimitedCrmClient(delegate, rl, rt);
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public List<FieldSchema> getSchema() {
        return execute(delegate::getSchema);
    }

    @Override
    public Optional<Contact> findContact(String email) {
        return execute(() -> delegate.findContact(email));
    }

    @Override
    public UpsertResult upsertContact(Contact contact) {
        return execute(() -> delegate.upsertContact(contact));
    }

    private <T> T execute(Supplier<T> call) {
        Supplier<T> throttled = RateLimiter.decorateSupplier(rateLimiter, () -> {
            try {
                return call.get();
            } catch (HttpStatusException hse) {
                if (hse.isRateLimited()) {
                    hse.retryAfter().ifPresent(this::sleep);
                }
                throw hse;
            }
        });
        return Retry.decorateSupplier(retry, throttled).get();
    }

    private void sleep(Duration d) {
        try {
            long ms = Math.max(0L, d.toMillis());
            if (ms > 0) Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
