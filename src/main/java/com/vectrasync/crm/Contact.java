package com.vectrasync.crm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record Contact(Map<String, Object> fields) {

    public Contact {
        Objects.requireNonNull(fields, "fields");
        fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public Object get(String field) {
        return fields.get(field);
    }
}
