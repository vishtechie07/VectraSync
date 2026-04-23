package com.vectrasync.crm.twenty;

import com.vectrasync.crm.Contact;
import com.vectrasync.crm.CrmClient;
import com.vectrasync.crm.FieldSchema;
import com.vectrasync.crm.UpsertResult;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TwentyClient implements CrmClient {

    private final String apiKey;
    private final String baseUrl;

    public TwentyClient(String baseUrl, String apiKey) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
    }

    @Override
    public String name() {
        return "Twenty";
    }

    @Override
    public List<FieldSchema> getSchema() {
        throw new UnsupportedOperationException("TwentyClient not yet implemented");
    }

    @Override
    public Optional<Contact> findContact(String email) {
        throw new UnsupportedOperationException("TwentyClient not yet implemented");
    }

    @Override
    public UpsertResult upsertContact(Contact contact) {
        throw new UnsupportedOperationException("TwentyClient not yet implemented");
    }
}
