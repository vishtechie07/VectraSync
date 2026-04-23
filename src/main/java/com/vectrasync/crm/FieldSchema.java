package com.vectrasync.crm;

public record FieldSchema(String name, FieldType type, boolean required) {

    public enum FieldType {
        STRING, EMAIL, PHONE, URL, NUMBER, BOOLEAN, DATE, REFERENCE, UNKNOWN
    }
}
