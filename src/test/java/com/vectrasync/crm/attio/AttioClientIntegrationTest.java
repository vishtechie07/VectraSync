package com.vectrasync.crm.attio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.vectrasync.crm.Contact;
import com.vectrasync.crm.HttpStatusException;
import com.vectrasync.crm.UpsertResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttioClientIntegrationTest {

    private WireMockServer wm;
    private AttioClient client;

    @BeforeEach
    void start() {
        wm = new WireMockServer(options().dynamicPort());
        wm.start();
        client = new AttioClient("test-key",
                "http://localhost:" + wm.port(),
                HttpClient.newHttpClient(),
                new ObjectMapper());
    }

    @AfterEach
    void stop() {
        if (wm != null) wm.stop();
    }

    @Test
    void upsertReturnsUpdatedOn200() {
        wm.stubFor(put(urlPathEqualTo("/objects/people/records"))
                .withQueryParam("matching_attribute", equalTo("email_addresses"))
                .withHeader("Authorization", equalTo("Bearer test-key"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"id\":{\"record_id\":\"rec_42\"}}}")));

        UpsertResult r = client.upsertContact(new Contact(Map.of("email_addresses", "a@b.com")));

        assertThat(r.status()).isEqualTo(UpsertResult.Status.UPDATED);
        assertThat(r.id()).isEqualTo("rec_42");
    }

    @Test
    void rateLimitedExceptionCarriesRetryAfter() {
        wm.stubFor(put(urlPathEqualTo("/objects/people/records"))
                .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "2").withBody("slow down")));

        assertThatThrownBy(() -> client.upsertContact(new Contact(Map.of("email_addresses", "a@b.com"))))
                .isInstanceOfSatisfying(HttpStatusException.class, ex -> {
                    assertThat(ex.isRateLimited()).isTrue();
                    assertThat(ex.retryAfter()).isPresent();
                    assertThat(ex.retryAfter().get().toSeconds()).isEqualTo(2);
                });
    }

    @Test
    void unauthorizedMapsTo401() {
        wm.stubFor(put(urlPathEqualTo("/objects/people/records"))
                .willReturn(aResponse().withStatus(401).withBody("bad token")));

        assertThatThrownBy(() -> client.upsertContact(new Contact(Map.of("email_addresses", "a@b.com"))))
                .isInstanceOfSatisfying(HttpStatusException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(401);
                    assertThat(ex.isUnauthorized()).isTrue();
                });
    }

    @Test
    void serverErrorSignalsRetryable() {
        wm.stubFor(put(urlPathEqualTo("/objects/people/records"))
                .willReturn(aResponse().withStatus(503).withBody("unavailable")));

        assertThatThrownBy(() -> client.upsertContact(new Contact(Map.of("email_addresses", "a@b.com"))))
                .isInstanceOfSatisfying(HttpStatusException.class, ex -> {
                    assertThat(ex.isServerError()).isTrue();
                });
    }

    @Test
    void getSchemaParsesAttributes() {
        wm.stubFor(get(urlPathEqualTo("/objects/people/attributes"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"data":[
                                  {"api_slug":"email_addresses","type":"email-address","is_required":true},
                                  {"api_slug":"name","type":"text","is_required":true},
                                  {"api_slug":"company_domain","type":"domain","is_required":false}
                                ]}""")));

        var schema = client.getSchema();
        assertThat(schema).hasSize(3);
        assertThat(schema.get(0).name()).isEqualTo("email_addresses");
        assertThat(schema.get(0).required()).isTrue();
    }
}
