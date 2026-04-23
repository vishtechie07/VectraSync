package com.vectrasync.crm.mock;

import com.vectrasync.crm.Contact;
import com.vectrasync.crm.UpsertResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MockCrmClientTest {

    private MockCrmClient client;

    @BeforeEach
    void setUp() {
        client = new MockCrmClient();
    }

    @Test
    void schemaIsNonEmpty() {
        assertThat(client.getSchema()).isNotEmpty();
    }

    @Test
    void upsertThenFind() {
        UpsertResult r1 = client.upsertContact(new Contact(Map.of(
                "email_addresses", "Ada@Example.COM",
                "name", "Ada Lovelace")));
        assertThat(r1.status()).isEqualTo(UpsertResult.Status.CREATED);

        UpsertResult r2 = client.upsertContact(new Contact(Map.of(
                "email_addresses", "ada@example.com",
                "name", "Ada L.")));
        assertThat(r2.status()).isEqualTo(UpsertResult.Status.UPDATED);

        assertThat(client.findContact("ada@example.com")).isPresent();
    }

    @Test
    void emptyFieldsFails() {
        UpsertResult r = client.upsertContact(new Contact(Map.of()));
        assertThat(r.status()).isEqualTo(UpsertResult.Status.FAILED);
    }

    @Test
    void upsertMileageRowWithSyntheticKey() {
        Map<String, Object> row = Map.of(
                "date", "2026-04-20",
                "destination", "Acme HQ",
                "start_odometer", "12000",
                "end_odometer", "12042",
                "distance", "42",
                "comment", "Client visit");

        UpsertResult r1 = client.upsertContact(new Contact(row));
        assertThat(r1.status()).isEqualTo(UpsertResult.Status.CREATED);

        UpsertResult r2 = client.upsertContact(new Contact(row));
        assertThat(r2.status()).isEqualTo(UpsertResult.Status.UPDATED);

        assertThat(client.size()).isEqualTo(1);
    }

    @Test
    void schemaIncludesMileageFields() {
        assertThat(client.getSchema())
                .extracting(s -> s.name())
                .contains("date", "destination", "start_odometer", "end_odometer", "distance", "comment");
    }
}
