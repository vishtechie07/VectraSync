package com.vectrasync.crm.mock;

import com.vectrasync.crm.Contact;
import com.vectrasync.crm.CrmClient;
import com.vectrasync.crm.FieldSchema;
import com.vectrasync.crm.FieldSchema.FieldType;
import com.vectrasync.crm.UpsertResult;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@VaadinSessionScope
public class MockCrmClient implements CrmClient {

    private static final List<FieldSchema> SCHEMA = List.of(
            new FieldSchema("email_addresses", FieldType.EMAIL, false),
            new FieldSchema("name", FieldType.STRING, false),
            new FieldSchema("phone_numbers", FieldType.PHONE, false),
            new FieldSchema("company_domain", FieldType.URL, false),
            new FieldSchema("date", FieldType.DATE, false),
            new FieldSchema("destination", FieldType.STRING, false),
            new FieldSchema("start_odometer", FieldType.NUMBER, false),
            new FieldSchema("end_odometer", FieldType.NUMBER, false),
            new FieldSchema("distance", FieldType.NUMBER, false),
            new FieldSchema("comment", FieldType.STRING, false)
    );

    private static final List<String> SYNTHETIC_KEY_FIELDS = List.of(
            "date", "destination", "start_odometer", "end_odometer");

    private final ConcurrentHashMap<String, Map<String, Object>> rows = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "Mock CRM";
    }

    @Override
    public List<FieldSchema> getSchema() {
        return SCHEMA;
    }

    @Override
    public Optional<Contact> findContact(String email) {
        String key = normalizeEmail(email);
        Map<String, Object> row = rows.get(key);
        if (row == null) return Optional.empty();
        return Optional.of(new Contact(new LinkedHashMap<>(row)));
    }

    @Override
    public UpsertResult upsertContact(Contact contact) {
        Map<String, Object> fields = contact.fields();
        if (fields.isEmpty()) {
            return UpsertResult.failed("No mapped fields — nothing to upsert.");
        }

        String email = extractEmail(fields);
        String key = (email != null) ? normalizeEmail(email) : syntheticKey(fields);
        boolean existed = rows.containsKey(key);

        Map<String, Object> merged = new LinkedHashMap<>(rows.getOrDefault(key, Map.of()));
        merged.putAll(fields);
        if (email != null) merged.putIfAbsent("email_addresses", email);
        rows.put(key, merged);

        return existed ? UpsertResult.updated(key) : UpsertResult.created(key);
    }

    public void clear() {
        rows.clear();
    }

    public int size() {
        return rows.size();
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String extractEmail(Map<String, Object> fields) {
        Object direct = fields.get("email_addresses");
        if (direct != null) {
            String s = String.valueOf(direct).trim();
            if (!s.isBlank()) return s;
        }
        for (Map.Entry<String, Object> e : fields.entrySet()) {
            if (e.getKey() != null && e.getKey().toLowerCase(Locale.ROOT).contains("email")) {
                String s = String.valueOf(e.getValue()).trim();
                if (s.contains("@")) return s;
            }
        }
        for (Object v : fields.values()) {
            String s = String.valueOf(v).trim();
            if (s.contains("@") && s.indexOf('@') > 0 && s.indexOf('@') < s.length() - 1) return s;
        }
        return null;
    }

    private static String syntheticKey(Map<String, Object> fields) {
        StringBuilder sb = new StringBuilder();
        for (String f : SYNTHETIC_KEY_FIELDS) {
            Object v = fields.get(f);
            if (v == null) continue;
            String s = String.valueOf(v).trim().toLowerCase(Locale.ROOT);
            if (s.isEmpty()) continue;
            if (sb.length() > 0) sb.append('|');
            sb.append(f).append('=').append(s);
        }
        if (sb.length() == 0) return "row-" + UUID.randomUUID();
        return sb.toString();
    }
}
