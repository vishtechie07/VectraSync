package com.vectrasync.crm.resilience;

import com.vectrasync.crm.Contact;
import com.vectrasync.crm.CrmClient;
import com.vectrasync.crm.FieldSchema;
import com.vectrasync.crm.HttpStatusException;
import com.vectrasync.crm.UpsertResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitedCrmClientTest {

    @Test
    void retriesOn429AndEventuallySucceeds() {
        AtomicInteger calls = new AtomicInteger();
        CrmClient flaky = new StubClient() {
            @Override public UpsertResult upsertContact(Contact contact) {
                if (calls.incrementAndGet() < 3) {
                    throw new HttpStatusException(429, Duration.ofMillis(50), "rate limited");
                }
                return UpsertResult.created("rec_1");
            }
        };
        CrmClient wrapped = RateLimitedCrmClient.wrap(flaky, 50);

        UpsertResult r = wrapped.upsertContact(new Contact(java.util.Map.of("email", "a@b.com")));

        assertThat(r.status()).isEqualTo(UpsertResult.Status.CREATED);
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void givesUpAfterMaxAttemptsAndPropagates() {
        AtomicInteger calls = new AtomicInteger();
        CrmClient alwaysLimited = new StubClient() {
            @Override public UpsertResult upsertContact(Contact contact) {
                calls.incrementAndGet();
                throw new HttpStatusException(429, Duration.ofMillis(10), "nope");
            }
        };
        CrmClient wrapped = RateLimitedCrmClient.wrap(alwaysLimited, 50);

        assertThatThrownBy(() -> wrapped.upsertContact(new Contact(java.util.Map.of("email", "a@b.com"))))
                .isInstanceOf(HttpStatusException.class);
        assertThat(calls.get()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void doesNotRetryOn401() {
        AtomicInteger calls = new AtomicInteger();
        CrmClient unauthorized = new StubClient() {
            @Override public UpsertResult upsertContact(Contact contact) {
                calls.incrementAndGet();
                throw new HttpStatusException(401, null, "bad token");
            }
        };
        CrmClient wrapped = RateLimitedCrmClient.wrap(unauthorized, 50);

        assertThatThrownBy(() -> wrapped.upsertContact(new Contact(java.util.Map.of("email", "a@b.com"))))
                .isInstanceOf(HttpStatusException.class);
        assertThat(calls.get()).isEqualTo(1);
    }

    private static abstract class StubClient implements CrmClient {
        @Override public String name() { return "stub"; }
        @Override public List<FieldSchema> getSchema() { return List.of(); }
        @Override public Optional<Contact> findContact(String email) { return Optional.empty(); }
    }
}
