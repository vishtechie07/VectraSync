package com.vectrasync.crm.attio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vectrasync.crm.Contact;
import com.vectrasync.crm.CrmClient;
import com.vectrasync.crm.FieldSchema;
import com.vectrasync.crm.FieldSchema.FieldType;
import com.vectrasync.crm.HttpStatusException;
import com.vectrasync.crm.UpsertResult;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AttioClient implements CrmClient {

    private static final String DEFAULT_BASE_URL = "https://api.attio.com/v2";
    private static final String OBJECT = "people";
    private static final String MATCHING_ATTRIBUTE = "email_addresses";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final String apiKey;
    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper json;

    public AttioClient(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL,
                HttpClient.newBuilder().connectTimeout(TIMEOUT).build(), new ObjectMapper());
    }

    public AttioClient(String apiKey, String baseUrl, HttpClient http, ObjectMapper json) {
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.baseUrl = stripTrailingSlash(Objects.requireNonNull(baseUrl, "baseUrl"));
        this.http = Objects.requireNonNull(http, "http");
        this.json = Objects.requireNonNull(json, "json");
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    @Override
    public String name() {
        return "Attio";
    }

    @Override
    public List<FieldSchema> getSchema() {
        HttpResponse<String> resp = send(builder("/objects/" + OBJECT + "/attributes").GET().build());
        JsonNode data = parse(resp).path("data");
        List<FieldSchema> out = new ArrayList<>();
        for (JsonNode attr : data) {
            String slug = attr.path("api_slug").asText();
            String type = attr.path("type").asText();
            boolean required = attr.path("is_required").asBoolean(false);
            if (!slug.isBlank()) {
                out.add(new FieldSchema(slug, mapType(type), required));
            }
        }
        return out;
    }

    @Override
    public Optional<Contact> findContact(String email) {
        ObjectNode body = json.createObjectNode();
        ObjectNode filter = body.putObject("filter");
        ObjectNode emailFilter = filter.putObject("email_addresses");
        emailFilter.put("$eq", email);
        body.put("limit", 1);

        HttpResponse<String> resp = send(builder("/objects/" + OBJECT + "/records/query")
                .POST(BodyPublishers.ofString(body.toString())).build());
        JsonNode first = parse(resp).path("data").path(0);
        if (first.isMissingNode() || first.isNull()) return Optional.empty();

        Map<String, Object> flat = flattenValues(first.path("values"));
        flat.put("id", first.path("id").path("record_id").asText());
        return Optional.of(new Contact(flat));
    }

    @Override
    public UpsertResult upsertContact(Contact contact) {
        ObjectNode body = json.createObjectNode();
        ObjectNode data = body.putObject("data");
        data.set("values", toAttioValues(contact.fields()));

        URI uri = URI.create(baseUrl + "/objects/" + OBJECT + "/records"
                + "?matching_attribute=" + MATCHING_ATTRIBUTE);
        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .PUT(BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> resp = send(req);
        JsonNode node = parse(resp).path("data");
        String id = node.path("id").path("record_id").asText(null);
        return resp.statusCode() == 201 ? UpsertResult.created(id) : UpsertResult.updated(id);
    }

    private HttpRequest.Builder builder(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
    }

    private HttpResponse<String> send(HttpRequest req) {
        try {
            HttpResponse<String> resp = http.send(req, BodyHandlers.ofString());
            int status = resp.statusCode();
            if (status >= 200 && status < 300) return resp;
            Duration retryAfter = parseRetryAfter(resp).orElse(null);
            throw new HttpStatusException(status, retryAfter, resp.body());
        } catch (IOException e) {
            throw new RuntimeException("Network error calling Attio", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted calling Attio", e);
        }
    }

    private JsonNode parse(HttpResponse<String> resp) {
        try {
            return json.readTree(resp.body());
        } catch (IOException e) {
            throw new RuntimeException("Invalid JSON from Attio", e);
        }
    }

    private static Optional<Duration> parseRetryAfter(HttpResponse<?> resp) {
        return resp.headers().firstValue("Retry-After").map(v -> {
            try {
                return Duration.ofSeconds(Long.parseLong(v.trim()));
            } catch (NumberFormatException ignored) {
                try {
                    Instant when = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(v.trim()));
                    return Duration.between(Instant.now(), when);
                } catch (Exception e) {
                    return Duration.ofSeconds(1);
                }
            }
        });
    }

    private static FieldType mapType(String attioType) {
        return switch (attioType) {
            case "text" -> FieldType.STRING;
            case "email-address" -> FieldType.EMAIL;
            case "phone-number" -> FieldType.PHONE;
            case "domain", "url" -> FieldType.URL;
            case "number", "currency", "rating" -> FieldType.NUMBER;
            case "checkbox" -> FieldType.BOOLEAN;
            case "date", "timestamp" -> FieldType.DATE;
            case "record-reference", "actor-reference" -> FieldType.REFERENCE;
            default -> FieldType.UNKNOWN;
        };
    }

    private ObjectNode toAttioValues(Map<String, Object> fields) {
        ObjectNode values = json.createObjectNode();
        for (Map.Entry<String, Object> e : fields.entrySet()) {
            if (e.getValue() == null) continue;
            ArrayNode arr = values.putArray(e.getKey());
            ObjectNode item = arr.addObject();
            String slug = e.getKey();
            String str = String.valueOf(e.getValue());
            if (slug.contains("email")) {
                item.put("email_address", str);
            } else if (slug.contains("phone")) {
                item.put("phone_number", str);
            } else if (slug.contains("domain")) {
                item.put("domain", str);
            } else if (slug.equals("name")) {
                item.put("full_name", str);
            } else {
                item.put("value", str);
            }
        }
        return values;
    }

    private static Map<String, Object> flattenValues(JsonNode values) {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        values.fields().forEachRemaining(entry -> {
            JsonNode first = entry.getValue().path(0);
            if (first.isMissingNode()) return;
            Object v = first.hasNonNull("value") ? first.get("value").asText()
                    : first.hasNonNull("email_address") ? first.get("email_address").asText()
                    : first.hasNonNull("phone_number") ? first.get("phone_number").asText()
                    : first.hasNonNull("domain") ? first.get("domain").asText()
                    : first.hasNonNull("full_name") ? first.get("full_name").asText()
                    : first.toString();
            out.put(entry.getKey(), v);
        });
        return out;
    }
}
